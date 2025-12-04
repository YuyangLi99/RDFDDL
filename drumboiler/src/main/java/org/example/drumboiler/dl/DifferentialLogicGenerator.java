package org.example.drumboiler.dl;

import org.example.drumboiler.config.BoilerConfig;
import org.example.drumboiler.config.OdeConfig;
import org.example.drumboiler.config.OdeEquation;
import org.example.drumboiler.config.StateShapeConfig;
import org.example.drumboiler.config.TransitionConfig;
import org.example.drumboiler.config.VariableConstraint;
import org.example.drumboiler.config.ModeConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts SHACL state descriptions into dL obligations for KeYmaera X.
 */
public class DifferentialLogicGenerator {

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.############");

    public List<TransitionResult> generateObligations(BoilerConfig config, Path outputDir) throws IOException {
        Objects.requireNonNull(config.getOde(), "ODE configuration is required for dL generation.");

        Map<String, StateShapeConfig> shapesByState = new HashMap<>();
        for (StateShapeConfig shape : config.getStateShapes()) {
            if (shape.getState() != null) {
                shapesByState.put(shape.getState(), shape);
            }
        }

        Map<String, String> varMapping = buildVariableMapping(config);
        Map<String, ModeConfig> modesById = new HashMap<>();
        for (ModeConfig mode : config.getModes()) {
            modesById.put(mode.getId(), mode);
        }

        List<TransitionConfig> transitions = new ArrayList<>();
        if (config.getTransitions() != null && !config.getTransitions().isEmpty()) {
            transitions.addAll(config.getTransitions());
        } else {
            // 自动枚举：对所有不同状态做笛卡尔积生成 isNext，若模式不同再加一条 ModeChange
            transitions.addAll(autoGenerateTransitions(shapesByState));
        }

        Files.createDirectories(outputDir);
        List<TransitionResult> results = new ArrayList<>();

        for (TransitionConfig transition : transitions) {
            String tType = transition.getType() == null ? "isNext" : transition.getType();
            StateShapeConfig source = shapesByState.get(transition.getFrom());
            StateShapeConfig target = shapesByState.get(transition.getTo());
            String phiSrc = replaceTokens(normalizeLogic(buildPredicate(source)), varMapping);
            String phiTgt = replaceTokens(normalizeLogic(buildPredicate(target)), varMapping);

            ModeConfig mode = null;
            if (source != null && source.getMode() != null) {
                mode = modesById.get(source.getMode());
            }

            if ("ModeChange".equalsIgnoreCase(tType)) {
                ModeConfig tgtMode = null;
                if (target != null && target.getMode() != null) {
                    tgtMode = modesById.get(target.getMode());
                }
                String formula = formatModeChange(phiSrc, phiTgt, mode, tgtMode, varMapping);
                writeProblem(outputDir, transition, new ArrayList<>(varMapping.values()), formula, results, "modechange_");
            } else {
            String hp = buildHybridProgramBlock(selectHybridProgram(config, mode, varMapping), phiSrc, phiTgt, mode, varMapping);
            // For FlowingOff mode, don't add extra dom (phiSrc|phiTgt) twice; hp already has domain if any
            String formula = formatProblem(phiSrc, hp, phiSrc);
            writeProblem(outputDir, transition, new ArrayList<>(varMapping.values()), formula, results, "obligation_");
            }
        }

        return results;
    }

    private List<TransitionConfig> autoGenerateTransitions(Map<String, StateShapeConfig> shapesByState) {
        List<String> states = new ArrayList<>(shapesByState.keySet());
        List<TransitionConfig> out = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            for (int j = 0; j < states.size(); j++) {
                if (i == j) continue;
                String from = states.get(i);
                String to = states.get(j);
                // always isNext candidate
                TransitionConfig t = new TransitionConfig();
                t.setFrom(from);
                t.setTo(to);
                t.setType("isNext");
                out.add(t);
                // if mode differs, also add ModeChange candidate
                StateShapeConfig sFrom = shapesByState.get(from);
                StateShapeConfig sTo = shapesByState.get(to);
                if (sFrom != null && sTo != null &&
                        sFrom.getMode() != null && sTo.getMode() != null &&
                        !sFrom.getMode().equals(sTo.getMode())) {
                    TransitionConfig mc = new TransitionConfig();
                    mc.setFrom(from);
                    mc.setTo(to);
                    mc.setType("ModeChange");
                    out.add(mc);
                }
            }
        }
        return out;
    }

    private String buildPredicate(StateShapeConfig shape) {
        if (shape == null || shape.getConstraints().isEmpty()) {
            return "true";
        }
        List<String> literals = new ArrayList<>();
        for (VariableConstraint constraint : shape.getConstraints()) {
            String var = constraint.getVariable();
            if (constraint.getHasValue() != null) {
                literals.add(var + " = " + format(constraint.getHasValue()));
            }
            if (constraint.getMinInclusive() != null) {
                literals.add(var + " >= " + format(constraint.getMinInclusive()));
            }
            if (constraint.getMaxInclusive() != null) {
                literals.add(var + " <= " + format(constraint.getMaxInclusive()));
            }
            if (constraint.getMinExclusive() != null) {
                literals.add(var + " > " + format(constraint.getMinExclusive()));
            }
            if (constraint.getMaxExclusive() != null) {
                literals.add(var + " < " + format(constraint.getMaxExclusive()));
            }
        }
        return literals.isEmpty() ? "true" : "(" + String.join(" & ", literals) + ")";
    }

    private String buildHybridProgram(OdeConfig ode, Map<String, String> varMapping) {
        if (ode == null || ode.getEquations().isEmpty()) {
            return "skip";
        }
        String equationPart = ode.getEquations().stream()
                .map(eq -> replaceTokens(eq.getVariable(), varMapping) + "' = " + replaceTokens(normalizeLogic(eq.getExpression()), varMapping))
                .collect(Collectors.joining(",\n      "));
        if (ode.getEvolutionDomain() != null && !ode.getEvolutionDomain().isBlank()) {
            return "{\n      " + equationPart + "\n      & (" + replaceTokens(normalizeLogic(ode.getEvolutionDomain()), varMapping) + ")\n    }";
        }
        return "{\n      " + equationPart + "\n    }";
    }

    private String format(double value) {
        return NUMBER_FORMAT.format(value);
    }

    private String buildProgramVariables(BoilerConfig config, Map<String, String> varMapping) {
        List<String> tracked = new ArrayList<>();
        if (config.getTrackedVariables() != null && !config.getTrackedVariables().isEmpty()) {
            tracked.addAll(config.getTrackedVariables());
        } else {
            for (StateShapeConfig shape : config.getStateShapes()) {
                for (VariableConstraint c : shape.getConstraints()) {
                    if (c.getVariable() != null) {
                        tracked.add(c.getVariable());
                    }
                }
            }
        }
        tracked = tracked.stream().distinct().toList();
        List<String> mapped = tracked.stream().map(v -> varMapping.getOrDefault(v, v)).toList();
        return "Real " + String.join(", ", mapped) + ";";
    }

    private String formatProblem(String phiSrc, String hybridProgram, String phiPost) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(phiSrc).append("\n");
        sb.append("  ->\n");
        sb.append("  [").append(hybridProgram).append("]\n");
        sb.append("  ").append(phiPost);
        return sb.toString();
    }

    private String formatModeChange(String phiSrc, String phiTgt, ModeConfig srcMode, ModeConfig tgtMode, Map<String, String> varMapping) {
        // Keep mode-change formula simple to avoid contradictory start conditions
        String endSrc = (srcMode != null && srcMode.getEndCondition() != null) ? replaceTokens(normalizeLogic(srcMode.getEndCondition()), varMapping) : "true";
        String startTgt = (tgtMode != null && tgtMode.getStartCondition() != null) ? replaceTokens(normalizeLogic(tgtMode.getStartCondition()), varMapping) : "true";
        endSrc = stripUnderscoreVars(endSrc);
        startTgt = stripUnderscoreVars(startTgt);
        return "  (" + phiSrc + " & " + endSrc + ")\n" +
                "  ->\n" +
                "  (" + phiTgt + " & " + startTgt + ")";
    }

    private String buildHybridProgramBlock(String hp, String phiSrc, String phiTgt, ModeConfig mode, Map<String, String> varMapping) {
        // Compose evolution domain; avoid duplicating phiSrc/phiTgt when hp already contains them
        String dom = null;
        if (!(mode != null && mode.getEvolutionDomain() != null && mode.getEvolutionDomain().isBlank())) {
            dom = "( " + phiSrc + " | " + phiTgt + " )";
            if (mode != null && mode.getEvolutionDomain() != null && !mode.getEvolutionDomain().isBlank()) {
                String modeDom = replaceTokens(normalizeLogic(mode.getEvolutionDomain()), varMapping);
                modeDom = stripUnderscoreVars(modeDom);
                dom = "(" + dom + " & (" + modeDom + "))";
            }
        }
        // If hp already has braces, inject domain conjunction; otherwise treat as skip
        if (hp.startsWith("{")) {
            // remove trailing "}" to insert the extra conjunction cleanly
            String trimmed = hp.substring(1, hp.length() - 1).trim();
            if (dom != null) {
                return "{\n      " + trimmed + "\n      & (" + dom + ")\n    }";
            } else {
                return "{\n      " + trimmed + "\n    }";
            }
        }
        if (dom != null) {
            return "{\n      " + hp + "\n      & (" + dom + ")\n    }";
        }
        return "{\n      " + hp + "\n    }";
    }

    private void writeProblem(Path outputDir, TransitionConfig transition, List<String> programVars, String formula, List<TransitionResult> results, String prefix) throws IOException {
        String filename = prefix + transition.getFrom() + "_to_" + transition.getTo() + ".kyx";
        Path obligationFile = outputDir.resolve(filename);
        String entryName = transition.getFrom() + "_to_" + transition.getTo();

        String programVarsStr = "Real " + String.join(", ", new LinkedHashSet<>(programVars)) + ";";

        List<String> lines = List.of(
                "ArchiveEntry \"" + entryName + "\"",
                "ProgramVariables",
                "  " + programVarsStr,
                "End.",
                "Problem",
                formula,
                "End.",
                "End."
        );
        Files.write(obligationFile, lines, StandardCharsets.UTF_8);
        results.add(new TransitionResult(transition.getFrom(), transition.getTo(), obligationFile, "NOT_VERIFIED"));
    }

    private String stripUnderscoreVars(String expr) {
        return expr
                .replaceAll("\\bT_S\\b", "Ts")
                .replaceAll("\\bp_S\\b", "Ps")
                .replaceAll("\\bTS\\b", "Ts")
                .replaceAll("\\bpS\\b", "Ps");
    }


    private String selectHybridProgram(BoilerConfig config, ModeConfig mode, Map<String, String> varMapping) {
        if (mode != null && mode.getEquations() != null && !mode.getEquations().isEmpty()) {
            return buildHybridProgram(toOdeConfig(mode), varMapping);
        }
        if (config.getOde() != null && config.getOde().getEquations() != null && !config.getOde().getEquations().isEmpty()) {
            return buildHybridProgram(config.getOde(), varMapping);
        }
        return "skip";
    }

    private OdeConfig toOdeConfig(ModeConfig mode) {
        OdeConfig ode = new OdeConfig();
        ode.setEquations(mode.getEquations());
        ode.setEvolutionDomain(mode.getEvolutionDomain());
        return ode;
    }

    private String normalizeLogic(String expr) {
        if (expr == null) {
            return "";
        }
        return expr
                .replace("/\\", "&")
                .replace("\\/", "|");
    }

    private Map<String, String> buildVariableMapping(BoilerConfig config) {
        LinkedHashMap<String, String> mapping = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();
        List<String> candidates = new ArrayList<>();
        if (config.getTrackedVariables() != null) {
            candidates.addAll(config.getTrackedVariables());
        }
        for (StateShapeConfig shape : config.getStateShapes()) {
            for (VariableConstraint c : shape.getConstraints()) {
                if (c.getVariable() != null) {
                    candidates.add(c.getVariable());
                }
            }
        }
        for (String raw : candidates) {
            if (mapping.containsKey(raw)) {
                continue;
            }
            String safe = sanitize(raw);
            String base = safe;
            int idx = 1;
            while (used.contains(safe)) {
                safe = base + idx;
                idx++;
            }
            mapping.put(raw, safe);
            used.add(safe);
        }
        return mapping;
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "v";
        }
        String cleaned = raw.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isEmpty()) {
            cleaned = "v";
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "v" + cleaned;
        }
        return cleaned;
    }

    private String replaceTokens(String expr, Map<String, String> mapping) {
        if (expr == null || expr.isBlank() || mapping.isEmpty()) {
            return expr;
        }
        String result = expr;
        for (Map.Entry<String, String> e : mapping.entrySet()) {
            String raw = e.getKey();
            String safe = e.getValue();
            result = result.replaceAll("\\b" + Pattern.quote(raw) + "\\b", safe);
        }
        return result;
    }
}
