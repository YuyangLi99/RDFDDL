package org.example.drumboiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.drumboiler.config.BoilerConfig;
import org.example.drumboiler.dl.DifferentialLogicGenerator;
import org.example.drumboiler.dl.TransitionResult;
import org.example.drumboiler.fmu.FmuParser;
import org.example.drumboiler.fmu.FmuVariable;
import org.example.drumboiler.proof.KyxProver;
import org.example.drumboiler.llm.DummyLlmAssistant;
import org.example.drumboiler.llm.LlmAssistant;
import org.example.drumboiler.rdf.RdfEdgeWriter;
import org.example.drumboiler.rdf.RdfShaclGenerator;
import org.example.drumboiler.stats.SimulationStats;
import org.example.drumboiler.stats.SimulationStatsExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * End-to-end driver that orchestrates the FMU → RDF/SHACL → dL generation flow.
 */
public class DrumBoilerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrumBoilerApp.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final FmuParser fmuParser = new FmuParser();
    private final RdfShaclGenerator rdfGenerator = new RdfShaclGenerator();
    private final DifferentialLogicGenerator dlGenerator = new DifferentialLogicGenerator();
    private final KyxProver kyxProver = new KyxProver();
    private final RdfEdgeWriter edgeWriter = new RdfEdgeWriter();
    private final LlmAssistant llmAssistant = new DummyLlmAssistant();

    public static void main(String[] args) {
        try {
            new DrumBoilerApp().run(args);
        } catch (Exception ex) {
            LOGGER.error("Pipeline execution failed: {}", ex.getMessage(), ex);
            System.exit(1);
        }
    }

    private void run(String[] args) throws IOException {
        Map<String, String> options = parseArgs(args);
        Path fmuPath = Paths.get(options.getOrDefault("fmu", "fmu/DrumBoiler.fmu"));
        Path configPath = Paths.get(options.getOrDefault("config", "config/drumboiler_config.json"));
        Path outputDir = Paths.get(options.getOrDefault("output", "build"));
        boolean verify = Boolean.parseBoolean(options.getOrDefault("verify", "false"));
        boolean suggest = options.containsKey("suggest-config");
        boolean explain = options.containsKey("explain-shapes");
        boolean runPipeline = Boolean.parseBoolean(options.getOrDefault("run-pipeline", "true"));
        boolean statsMode = options.containsKey("stats-from-csv");
        boolean suggestFromStats = options.containsKey("suggest-config-from-csv");
        boolean fitOde = options.containsKey("fit-ode-from-csv");

        LOGGER.info("Using FMU {}", fmuPath.toAbsolutePath());
        LOGGER.info("Using config {}", configPath.toAbsolutePath());

        if (!Files.exists(fmuPath)) {
            throw new IOException("FMU file not found: " + fmuPath.toAbsolutePath());
        }

        // Suggest config mode
        if (suggest) {
            runSuggestConfig(options, fmuPath);
            if (!runPipeline) {
                return;
            }
        }

        if (!Files.exists(configPath)) {
            throw new IOException("Config file not found: " + configPath.toAbsolutePath());
        }

        BoilerConfig config = mapper.readValue(configPath.toFile(), BoilerConfig.class);
        List<FmuVariable> variables = fmuParser.parseModelDescription(fmuPath);
        LOGGER.info("Parsed {} ScalarVariables from FMU", variables.size());

        if (statsMode) {
            runStatsFromCsv(options, config);
            if (!runPipeline && !suggestFromStats) {
                return;
            }
        }

        if (suggestFromStats) {
            runSuggestFromStats(options, config);
            if (!runPipeline) {
                return;
            }
        }

        if (fitOde) {
            runFitOdeFromCsv(options);
            if (!runPipeline) {
                return;
            }
        }

        // Explain shapes mode
        if (explain) {
            runExplainShapes(options);
            if (!runPipeline) {
                return;
            }
        }

        Path rdfFile = outputDir.resolve("drumboiler_shapes.ttl");
        rdfGenerator.generateRdfShacl(variables, config, rdfFile);
        LOGGER.info("Wrote RDF/SHACL skeleton to {}", rdfFile.toAbsolutePath());

        Path obligationsDir = outputDir.resolve("obligations");
        List<TransitionResult> results = dlGenerator.generateObligations(config, obligationsDir);
        LOGGER.info("Generated {} dL obligations under {}", results.size(), obligationsDir.toAbsolutePath());

        if (verify) {
            results = verifyWithKyx(results);
        }

        Path summaryFile = outputDir.resolve("verification_summary.json");
        writeSummary(summaryFile, results);
        LOGGER.info("Summary written to {}", summaryFile.toAbsolutePath());

        if (verify) {
            Path nextEdgesFile = outputDir.resolve("drumboiler_next.ttl");
            edgeWriter.writeNextEdges(results, nextEdgesFile);
            LOGGER.info("Materialized refutable obligations as :next edges to {}", nextEdgesFile.toAbsolutePath());
        }
    }

    private Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        Set<String> flagsNoValue = Set.of("fit-ode-from-csv", "explain-shapes");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                if (flagsNoValue.contains(key)) {
                    options.put(key, "true");
                } else {
                    String value = i + 1 < args.length ? args[i + 1] : "";
                    options.put(key, value);
                    i++;
                }
            }
        }
        return options;
    }

    private void runSuggestConfig(Map<String, String> options, Path fmuPath) throws IOException {
        String descPath = options.get("suggest-config");
        if (descPath == null) {
            throw new IOException("--suggest-config requires a text file path with process description");
        }
        Path desc = Paths.get(descPath);
        if (!Files.exists(desc)) {
            throw new IOException("Description file not found: " + desc.toAbsolutePath());
        }
        List<FmuVariable> variables = fmuParser.parseModelDescription(fmuPath);
        String descriptionText = Files.readString(desc);
        String suggested = llmAssistant.suggestConfig(variables, descriptionText);
        Path out = Paths.get(options.getOrDefault("suggest-output", "config/drumboiler_config_suggested.json"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, suggested);
        LOGGER.info("Suggested config written to {}", out.toAbsolutePath());
    }

    private void runExplainShapes(Map<String, String> options) throws IOException {
        Path shapesPath = Paths.get(options.getOrDefault("shapes", "build/drumboiler_shapes.ttl"));
        if (!Files.exists(shapesPath)) {
            throw new IOException("SHACL file not found for explanation: " + shapesPath.toAbsolutePath());
        }
        var model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        try (var in = Files.newInputStream(shapesPath)) {
            org.apache.jena.riot.RDFDataMgr.read(model, in, shapesPath.toUri().toString(), org.apache.jena.riot.Lang.TURTLE);
        }
        String md = llmAssistant.summarizeShapes(model);
        Path out = Paths.get(options.getOrDefault("explain-output", "build/state_shapes.md"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, md);
        LOGGER.info("State shapes explanation written to {}", out.toAbsolutePath());
    }

    private void runStatsFromCsv(Map<String, String> options, BoilerConfig config) throws IOException {
        String csv = options.get("stats-from-csv");
        if (csv == null) {
            throw new IOException("--stats-from-csv requires a CSV path");
        }
        Path csvPath = Paths.get(csv);
        if (!Files.exists(csvPath)) {
            throw new IOException("CSV file not found: " + csvPath.toAbsolutePath());
        }
        SimulationStatsExtractor extractor = new SimulationStatsExtractor();
        Map<String, SimulationStats> stats = extractor.extract(csvPath, config.getTrackedVariables());
        LOGGER.info("Simulation stats from {}", csvPath.toAbsolutePath());
        stats.forEach((k, v) -> LOGGER.info("{}: min={}, max={}, mean={}, p33={}, p66={}, n={}",
                k, v.getMin(), v.getMax(), v.getMean(), v.getP33(), v.getP66(), v.getCount()));
        Path out = Paths.get(options.getOrDefault("stats-output", "build/stats.json"));
        Files.createDirectories(out.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), stats);
        LOGGER.info("Stats written to {}", out.toAbsolutePath());
    }

    private void runFitOdeFromCsv(Map<String, String> options) throws IOException {
        String csv = options.getOrDefault("csv", options.get("fit-ode-from-csv"));
        if (csv == null) {
            throw new IOException("--fit-ode-from-csv requires --csv <path>");
        }
        Path csvPath = Paths.get(csv);
        if (!Files.exists(csvPath)) {
            throw new IOException("CSV file not found: " + csvPath.toAbsolutePath());
        }
        var fit = org.example.drumboiler.stats.DrumBoilerOdeFitter.fitOdeFromCsv(csvPath);
        String formatted = String.format(
                "Ts'  = %.4f + %.4f*Ts + %.4f*Ps%n" +
                        "Ps'  = %.4f + %.4f*Ts + %.4f*Ps%n" +
                        "V_l'  = %.4f + %.4f*V_l + %.4f*qm_S%n" +
                        "qm_S' = 0",
                fit.aT, fit.bT, fit.cT,
                fit.aP, fit.bP, fit.cP,
                fit.aV, fit.bV, fit.cV);
        LOGGER.info("ODE fit from {}:%n{}", csvPath.toAbsolutePath(), formatted);
        Path out = Paths.get(options.getOrDefault("ode-fit-output", "build/ode_fit.txt"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, formatted);
        LOGGER.info("ODE fit written to {}", out.toAbsolutePath());
    }

    private void runSuggestFromStats(Map<String, String> options, BoilerConfig config) throws IOException {
        String csv = options.get("suggest-config-from-csv");
        if (csv == null) {
            throw new IOException("--suggest-config-from-csv requires a CSV path");
        }
        Path csvPath = Paths.get(csv);
        if (!Files.exists(csvPath)) {
            throw new IOException("CSV file not found: " + csvPath.toAbsolutePath());
        }
        SimulationStatsExtractor extractor = new SimulationStatsExtractor();
        Map<String, SimulationStats> stats = extractor.extract(csvPath, config.getTrackedVariables());
        Path out = Paths.get(options.getOrDefault("suggest-output", "config/drumboiler_config_from_stats.json"));
        Files.createDirectories(out.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), buildConfigFromStats(config, stats));
        LOGGER.info("Suggested config (from stats) written to {}", out.toAbsolutePath());
    }

    private Map<String, Object> buildConfigFromStats(BoilerConfig baseConfig, Map<String, SimulationStats> stats) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("trackedVariables", baseConfig.getTrackedVariables());
        List<Object> stateShapes = new ArrayList<>();
        stateShapes.add(stateFromStats("sLow", "Low region", stats, 0.0, 0.33));
        stateShapes.add(stateFromStats("sMid", "Mid region", stats, 0.33, 0.66));
        stateShapes.add(stateFromStats("sHigh", "High region", stats, 0.66, 1.0));
        root.put("stateShapes", stateShapes);
        List<Object> transitions = new ArrayList<>();
        transitions.add(Map.of("from", "sLow", "to", "sMid"));
        transitions.add(Map.of("from", "sMid", "to", "sHigh"));
        root.put("transitions", transitions);
        if (baseConfig.getModes() != null && !baseConfig.getModes().isEmpty()) {
            root.put("modes", baseConfig.getModes());
        } else if (baseConfig.getOde() != null) {
            root.put("ode", baseConfig.getOde());
        }
        return root;
    }

    private Map<String, Object> stateFromStats(String id, String label, Map<String, SimulationStats> stats, double low, double high) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id + "Shape");
        node.put("state", id);
        node.put("label", label);
        List<Object> constraints = new ArrayList<>();
        stats.forEach((var, st) -> {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("variable", var);
            double lo = st.getP33();
            double hi = st.getP66();
            if (Double.isNaN(lo)) lo = st.getMin();
            if (Double.isNaN(hi)) hi = st.getMax();
            double min = switchRegion(low, high, st, lo, hi, true);
            double max = switchRegion(low, high, st, lo, hi, false);
            if (!Double.isNaN(min)) c.put("minInclusive", min);
            if (!Double.isNaN(max)) c.put("maxInclusive", max);
            constraints.add(c);
        });
        node.put("constraints", constraints);
        return node;
    }

    private double switchRegion(double low, double high, SimulationStats st, double p33, double p66, boolean isMin) {
        if (st.getCount() == 0) return Double.NaN;
        if (high <= 0.34) {
            return isMin ? st.getMin() : p33;
        } else if (low >= 0.66) {
            return isMin ? p66 : st.getMax();
        } else {
            return isMin ? p33 : p66;
        }
    }

    private void writeSummary(Path file, List<TransitionResult> results) throws IOException {
        Files.createDirectories(file.getParent());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("timestamp", System.currentTimeMillis());
        summary.put("obligations", results.stream()
                .map(result -> Map.of(
                        "from", result.getFrom(),
                        "to", result.getTo(),
                        "status", result.getStatus(),
                        "file", result.getObligationFile().toString()
                ))
                .collect(Collectors.toList()));
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), summary);
    }

    private List<TransitionResult> verifyWithKyx(List<TransitionResult> results) {
        LOGGER.info("Verifying obligations via KeYmaera X at http://localhost:8090 ...");
        List<TransitionResult> updated = new ArrayList<>();
        for (TransitionResult result : results) {
            String kyxContent;
            try {
                kyxContent = Files.readString(result.getObligationFile());
            } catch (IOException e) {
                LOGGER.warn("Failed to read {}: {}", result.getObligationFile(), e.getMessage());
                updated.add(result.withStatus("error:read"));
                continue;
            }
            String status;
            try {
                status = kyxProver.prove(kyxContent);
            } catch (Exception ex) {
                LOGGER.warn("Verification failed for {} -> {}: {}", result.getFrom(), result.getTo(), ex.getMessage());
                status = "error:exception";
            }
            LOGGER.info("Obligation {} -> {} : {}", result.getFrom(), result.getTo(), status);
            updated.add(result.withStatus(status));
        }
        return updated;
    }
}
