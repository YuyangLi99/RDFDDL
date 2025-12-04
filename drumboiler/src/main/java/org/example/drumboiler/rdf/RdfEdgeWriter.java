package org.example.drumboiler.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.example.drumboiler.dl.TransitionResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes verification results back into an RDF graph as :next edges.
 * Only obligations marked as refutable ("false") are materialized.
 */
public class RdfEdgeWriter {

    /**
     * Persist verified transitions to a TTL file.
     *
     * @param results verification outcomes
     * @param output  TTL path to write
     */
    public void writeNextEdges(List<TransitionResult> results, Path output) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("pre", DrumBoilerVocabulary.NS);
        for (TransitionResult result : results) {
            if (!"false".equalsIgnoreCase(result.getStatus())) {
                continue; // we only materialize refutable safety obligations = reachable edges
            }
            Resource from = model.createResource(DrumBoilerVocabulary.NS + result.getFrom());
            Resource to = model.createResource(DrumBoilerVocabulary.NS + result.getTo());
            model.add(from, DrumBoilerVocabulary.NEXT, to);
        }
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        RDFDataMgr.write(Files.newOutputStream(output), model, RDFFormat.TURTLE_PRETTY);
    }
}
