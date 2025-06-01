package org.example;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ModeChange4:
 * (s1) must be s212
 * s212 must contain HomogenizerOffMode
 * The second argument (s2) must be a state that has the same x and p constraints as s212
 * Perform a single KeYmaeraX proof: (s212.xRange) -> (HomogenizerOnMode.startingCondition)
 * If Prove_helper.prove(...) returns "true", this built-in returns true
 */
public class ModeChange4 extends BaseBuiltin {

    private final Dataset dataset;

    public ModeChange4(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public String getName() {
        return "mode_change4";
    }

    @Override
    public int getArgLength() {
        // The rule expects 2 arguments: state1, state2
        return 2;
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        // Require that both arguments are URIs
        if (!args[0].isURI() || !args[1].isURI()) {
            return false;
        }
        String s1Uri = args[0].getURI();
        String s2Uri = args[1].getURI();

        // Only process if s1 = s212
        if (!"https://anonymous.example.org#s212".equals(s1Uri)) {
            return false;
        }

        // Verify that s1 and s2 are different states
        if (s1Uri.equals(s2Uri)) {
            return false;
        }

        // Retrieve the default model
        Model model = dataset.getDefaultModel();
        Resource s1 = model.getResource(s1Uri);
        Resource s2 = model.getResource(s2Uri);

        // s212 must contain HomogenizerOffMode
        if (!stateHasHomogenizerOffMode(s1Uri)) {
            System.out.println("s212 does not have HomogenizerOffMode");
            return false;
        }

        // Check if s2 and s212 have the same x and P constraints
        if (!statesHaveSameConstraints(s1Uri, s2Uri)) {
            System.out.println("s2 does not have the same constraints as s212");
            return false;
        }

        // Get s212's x range constraints
        Map<String, Object> s212Constraints = getStateXPConstraints(s1Uri);
        String xMin = (String) s212Constraints.get("x_min");
        String xMax = (String) s212Constraints.get("x_max");

        // Build x range expression
        StringBuilder xRangeBuilder = new StringBuilder();
        if (xMin != null && !xMin.isEmpty()) {
            xRangeBuilder.append("x>=").append(xMin);
        }
        if (xMax != null && !xMax.isEmpty()) {
            if (xRangeBuilder.length() > 0) {
                xRangeBuilder.append(" & ");
            }
            xRangeBuilder.append("x<=").append(xMax);
        }
        String xRange = xRangeBuilder.toString();

        if (xRange.isEmpty()) {
            System.out.println("Failed to extract x range constraints for s212");
            return false;
        }

        // Get HomogenizerOnMode's startingCondition
        String onModeStartCond = getHomogenizerOnModeStartCondition();
        if (onModeStartCond == null || onModeStartCond.isEmpty()) {
            System.out.println("Failed to get HomogenizerOnMode starting condition");
            return false;
        }

        // Perform proof: (s212.xRange) -> (HomogenizerOnMode.startingCondition)
        System.out.println("Performing proof: (" + xRange + ") -> (" + onModeStartCond + ")");
        String proofInput = buildProofRequest(xRange, onModeStartCond);
        String result = Prove_helper.prove(proofInput);
        System.out.println("Proof result: " + result);

        // If the proof is successful, check if s2 has HomogenizerOnMode
        if ("true".equals(result)) {
            boolean hasOnMode = stateHasHomogenizerOnMode(s2Uri);
            System.out.println("State " + s2Uri + " has HomogenizerOnMode: " + hasOnMode);

            if (hasOnMode) {
                // Create mode change relationship between s1 and s2
                Property modeChangeProperty = model.createProperty("http://example.org/states#ModeChange4");
                s1.addProperty(modeChangeProperty, s2);
                return true;
            }
        }

        return false;
    }

    // ================== Helper Methods ==================

    /**
     * Get x and P constraints for a state
     */
    private Map<String, Object> getStateXPConstraints(String stateUri) {
        Map<String, Object> constraints = new HashMap<>();

        String queryString =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                        "SELECT ?variable ?minVal ?maxVal WHERE {\n" +
                        "  BIND(<" + stateUri + "> AS ?s)\n" +
                        "  ?s pre:hasShape ?sShape .\n" +
                        "  ?sShape sh:property [ sh:node ?deviceSubShape ] .\n" +
                        "  ?deviceSubShape sh:property ?propNode .\n" +
                        "  ?propNode sh:path ?variable .\n" +
                        "  FILTER (?variable IN (pre:x, pre:P))\n" +
                        "  OPTIONAL { ?propNode sh:minInclusive ?minVal }\n" +
                        "  OPTIONAL { ?propNode sh:maxInclusive ?maxVal }\n" +
                        "}\n" +
                        "ORDER BY ?variable";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();

                String varName = soln.getResource("variable").getLocalName();

                if (soln.contains("minVal")) {
                    constraints.put(varName + "_min", soln.getLiteral("minVal").getString());
                }

                if (soln.contains("maxVal")) {
                    constraints.put(varName + "_max", soln.getLiteral("maxVal").getString());
                }
            }
        }

        return constraints;
    }

    /**
     * Get HomogenizerOnMode's startingCondition
     */
    private String getHomogenizerOnModeStartCondition() {
        String queryString =
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "SELECT ?startCond WHERE {\n" +
                        "  pre:HomogenizerOnMode pre:hasODE ?odeBlank .\n" +
                        "  ?odeBlank rdf:type pre:ODE ;\n" +
                        "           pre:startingCondition ?startCond .\n" +
                        "}";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                return results.next().getLiteral("startCond").getString();
            }
        }

        return null;
    }

    // ================== Helper Methods ==================

    /**
     * Check if a state has HomogenizerOffMode
     */
    private boolean stateHasHomogenizerOffMode(String stateUri) {
        String queryString =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "ASK {\n" +
                        "  BIND(<" + stateUri + "> AS ?state)\n" +
                        "  ?state pre:hasShape ?stateShape .\n" +
                        "  ?stateShape sh:property [ \n" +
                        "    sh:path pre:hasHomogenizer ;\n" +
                        "    sh:node ?homogenizerShape \n" +
                        "  ] .\n" +
                        "  ?homogenizerShape sh:property [ \n" +
                        "    sh:path pre:mode ;\n" +
                        "    sh:hasValue pre:OffMode\n" +
                        "  ] .\n" +
                        "}";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            return qexec.execAsk();
        }
    }

    /**
     * Check if a state has HomogenizerOnMode
     */
    private boolean stateHasHomogenizerOnMode(String stateUri) {
        String queryString =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "ASK {\n" +
                        "  BIND(<" + stateUri + "> AS ?state)\n" +
                        "  ?state pre:hasShape ?stateShape .\n" +
                        "  ?stateShape sh:property [ \n" +
                        "    sh:path pre:hasHomogenizer ;\n" +
                        "    sh:node ?homogenizerShape \n" +
                        "  ] .\n" +
                        "  ?homogenizerShape sh:property [ \n" +
                        "    sh:path pre:mode ;\n" +
                        "    sh:hasValue pre:OnMode\n" +
                        "  ] .\n" +
                        "}";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            return qexec.execAsk();
        }
    }

    /**
     * Check if two states have the same x and P constraints
     */
    private boolean statesHaveSameConstraints(String state1Uri, String state2Uri) {
        // Get constraints for both states
        String queryString1 = createConstraintQuery(state1Uri);
        String queryString2 = createConstraintQuery(state2Uri);

        // Execute queries and compare results
        try {
            QueryExecution qexec1 = QueryExecutionFactory.create(QueryFactory.create(queryString1), dataset);
            QueryExecution qexec2 = QueryExecutionFactory.create(QueryFactory.create(queryString2), dataset);

            ResultSet results1 = qexec1.execSelect();
            ResultSet results2 = qexec2.execSelect();

            // Convert results to strings for comparison
            String result1 = ResultSetFormatter.asText(results1);
            qexec1.close();

            String result2 = ResultSetFormatter.asText(results2);
            qexec2.close();

            // Print results for debugging
//            System.out.println("State1 constraints: " + result1);
//            System.out.println("State2 constraints: " + result2);

            // Compare results
            return result1.equals(result2);
        } catch (Exception e) {
            System.err.println("Error comparing constraints: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create a SPARQL query to get state constraints
     */
    private String createConstraintQuery(String stateUri) {
        return "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX pre: <https://anonymous.example.org#>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "SELECT ?variable ?minVal ?maxVal WHERE {\n" +
                "  BIND(<" + stateUri + "> AS ?s)\n" +
                "  ?s pre:hasShape ?sShape .\n" +
                "  ?sShape sh:property [ sh:node ?deviceSubShape ] .\n" +
                "  ?deviceSubShape sh:property ?propNode .\n" +
                "  ?propNode sh:path ?variable .\n" +
                "  FILTER (?variable IN (pre:x, pre:P))\n" +
                "  OPTIONAL { ?propNode sh:minInclusive ?minVal }\n" +
                "  OPTIONAL { ?propNode sh:maxInclusive ?maxVal }\n" +
                "}\n" +
                "ORDER BY ?variable";
    }

    /**
     * Build a KeYmaeraX proof request
     */
    private String buildProofRequest(String xRange, String startingCondition) {
        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n");
        sb.append("Real x;\n");
        sb.append("End.\nProblem\n");
        sb.append("(").append(xRange).append(") -> (").append(startingCondition).append(")\n");
        sb.append("End.\nEnd.");
        return sb.toString();
    }
}
