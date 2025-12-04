package org.example.drumboiler.reasoner;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.Lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Jena rule runner for drumboiler, mirroring the Oven example:
 * - loads a TTL (default build/drumboiler_shapes.ttl)
 * - registers isNext/mode_change builtins
 * - runs two rules to materialize :next and :ModeChange
 * Note: Builtins will call KeYmaeraX via Prove_helper, so ensure the server is running.
 */
public class ReasonerRunner {
    public static void main(String[] args) throws IOException {
        Path ttlPath = args.length > 0 ? Path.of(args[0]) : Path.of("build/drumboiler_shapes.ttl");
        Path outPath = args.length > 1 ? Path.of(args[1]) : Path.of("build/reasoner_output.ttl");

        if (!Files.exists(ttlPath)) {
            throw new IOException("TTL not found: " + ttlPath.toAbsolutePath());
        }

        Dataset ds = DatasetFactory.create();
        Model model = ds.getDefaultModel();
        // Jena 5 signature: (Model, InputStream, Lang)
        RDFDataMgr.read(model, Files.newInputStream(ttlPath), Lang.TURTLE);

        // register builtins (isNext + mode_change)
        ReasonerSetup.registerBuiltins(ds);

        String rules =
                "[rule1: (?a <http://example.org/states#next> ?b) <- "
                        + "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                        + "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                        + " isNext(?a, ?b) ] "
                        + "[rule2: (?a <http://example.org/states#ModeChange> ?b) <- "
                        + "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                        + "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                        + " mode_change(?a, ?b) ]";

        List<Rule> parsed = Rule.parseRules(rules);
        Reasoner reasoner = new GenericRuleReasoner(parsed);
        reasoner.setDerivationLogging(true);
        InfModel inf = ModelFactory.createInfModel(reasoner, model);

        Files.createDirectories(outPath.getParent());
        RDFDataMgr.write(Files.newOutputStream(outPath), inf, RDFFormat.TURTLE_PRETTY);
        System.out.println("Inference written to: " + outPath.toAbsolutePath());
    }
}
