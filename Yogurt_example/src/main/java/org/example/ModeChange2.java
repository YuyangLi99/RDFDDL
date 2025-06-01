package org.example;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

import java.util.*;

public class ModeChange2 extends BaseBuiltin {

    private Dataset dataset;

    public ModeChange2(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public String getName() {
        return "mode_change2";
    }

    @Override
    public int getArgLength() {
        return 2; // state1, state2
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        // Check if both parameters are URI
        if (!args[0].isURI() || !args[1].isURI()) {
            System.out.println("Arguments are not URIs");
            return false;
        }

        String s1Uri = args[0].getURI();
        String s2Uri = args[1].getURI();
        System.out.println("Processing transition: " + s1Uri + " -> " + s2Uri);

        // Only when it starts at s322, we go to the next steps
        if (!"https://anonymous.example.org#s322".equals(s1Uri)) {
            System.out.println("Source state is not s322, skipping");
            return false;
        }

        // 1. Extract constraints from s322
        Map<Double, Double> s322XRange = extractStateXRange(s1Uri);
        if (s322XRange == null) {
            System.out.println("Failed to extract x range from s322");
            return false;
        }

        double s322Min = s322XRange.keySet().iterator().next();
        double s322Max = s322XRange.values().iterator().next();
        String s322XRangeStr = "x >= " + s322Min + " & x <= " + s322Max;
        System.out.println("s322 x range: " + s322XRangeStr);

        // 2. Check if s322 has HeaterOffMode
        boolean hasHeaterOffMode = checkStateHasHeaterOffMode(s1Uri);
        if (!hasHeaterOffMode) {
            System.out.println("s322 does not have HeaterOffMode");
            return false;
        }

        // 3. Get all states with pasteurization modes
        Map<String, PasteurizationModeInfo> stateModesMap = getStatesPasteurizationModes();
        if (stateModesMap.isEmpty()) {
            System.out.println("No states with pasteurization modes found");
            return false;
        }

        // Check if destination state has a pasteurization mode
        if (!stateModesMap.containsKey(s2Uri)) {
            System.out.println("Destination state does not have a pasteurization mode");
            return false;
        }

        // 4. Get all unique pasteurization modes for testing
        List<PasteurizationModeInfo> uniqueModes = new ArrayList<>();
        Set<String> addedModes = new HashSet<>();

        for (PasteurizationModeInfo info : stateModesMap.values()) {
            if (!addedModes.contains(info.pasteurModeUri)) {
                uniqueModes.add(info);
                addedModes.add(info.pasteurModeUri);
            }
        }

        // 5. Find a mode that passes the first proof
        PasteurizationModeInfo chosenMode = null;

        for (PasteurizationModeInfo mode : uniqueModes) {
            System.out.println("Testing mode: " + mode.pasteurModeUri);

            String proof1 = buildFirstProofRequest(s322XRangeStr, mode.startingCondition);
            System.out.println("KeymaeraX First Proof Request:");
            System.out.println(proof1);

            String result = Prove_helper.prove(proof1);
            System.out.println("Proof result: " + result);

            if ("true".equals(result)) {
                chosenMode = mode;
                System.out.println("Selected mode: " + mode.pasteurModeUri);
                break;
            }
        }

        if (chosenMode == null) {
            System.out.println("No suitable mode found");
            return false;
        }

        // 6. Check if destination state has the compatible mode
        PasteurizationModeInfo s2ModeInfo = stateModesMap.get(s2Uri);
        if (!s2ModeInfo.modeUri.equals(chosenMode.modeUri)) {
            System.out.println("Destination state does not have the required mode: " + chosenMode.modeUri);
            return false;
        }

        System.out.println("Destination state has compatible mode: " + s2ModeInfo.modeUri);

        // 7. Extract constraints from destination state
        Map<Double, Double> s2XRange = extractStateXRange(s2Uri);
        if (s2XRange == null) {
            System.out.println("Failed to extract x range from destination state");
            return false;
        }

        double s2Min = s2XRange.keySet().iterator().next();
        double s2Max = s2XRange.values().iterator().next();
        String s2XRangeStr = "x >= " + s2Min + " & x <= " + s2Max;
        System.out.println("Destination state x range: " + s2XRangeStr);

        // 8. Build and run second proof
        String ode = "x' = " + chosenMode.derivative;
        String proof2 = buildSecondProofRequest(s322XRangeStr, ode, chosenMode.domainConstraint, s2XRangeStr);
        System.out.println("KeymaeraX Second Proof Request:");
        System.out.println(proof2);

        String result2 = Prove_helper.prove(proof2);
        System.out.println("Second proof result: " + result2);

        // According to the convention, "false" indicates success
        boolean success = "false".equals(result2);
        System.out.println("Transition verification " + (success ? "succeeded" : "failed"));
        return success;
    }

    /**
     * Extract x range constraints from a state
     * @return Map with min value as key and max value as value
     */
    private Map<Double, Double> extractStateXRange(String stateUri) {
        String queryStr =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "SELECT ?minVal ?maxVal WHERE {\n" +
                        "  <" + stateUri + "> pre:hasShape ?shape .\n" +
                        "  ?shape sh:property ?prop .\n" +
                        "  ?prop sh:node ?deviceShape .\n" +
                        "  ?deviceShape sh:property ?xProp .\n" +
                        "  ?xProp sh:path pre:x .\n" +
                        "  OPTIONAL { ?xProp sh:minInclusive ?minVal }\n" +
                        "  OPTIONAL { ?xProp sh:maxInclusive ?maxVal }\n" +
                        "}";

        System.out.println("Executing SPARQL query for state x range...");

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                // Get min and max values
                double minVal = solution.get("minVal") != null ?
                        solution.get("minVal").asLiteral().getDouble() : Double.NEGATIVE_INFINITY;
                double maxVal = solution.get("maxVal") != null ?
                        solution.get("maxVal").asLiteral().getDouble() : Double.POSITIVE_INFINITY;

                Map<Double, Double> range = new HashMap<>();
                range.put(minVal, maxVal);
                return range;
            }
        } catch (Exception e) {
            System.err.println("Error querying state x range: " + e.getMessage());
        }

        return null;
    }

    /**
     * Check if a state has HeaterOffMode
     */
    private boolean checkStateHasHeaterOffMode(String stateUri) {
        String queryStr =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "ASK {\n" +
                        "  <" + stateUri + "> pre:hasShape ?shape .\n" +
                        "  ?shape sh:property ?prop .\n" +
                        "  ?prop sh:path pre:hasHeater .\n" +
                        "  ?prop sh:node ?heaterShape .\n" +
                        "  ?heaterShape sh:property ?modeProp .\n" +
                        "  ?modeProp sh:path pre:mode .\n" +
                        "  ?modeProp sh:hasValue pre:OffMode .\n" +
                        "}";

        System.out.println("Checking if state has HeaterOffMode...");

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            return qexec.execAsk();
        } catch (Exception e) {
            System.err.println("Error checking HeaterOffMode: " + e.getMessage());
            return false;
        }
    }



    /**
     * Get information about states with pasteurization modes
     * @return Map of state URIs to their compatible pasteurization modes
     */
    private Map<String, PasteurizationModeInfo> getStatesPasteurizationModes() {
        Map<String, PasteurizationModeInfo> stateModesMap = new HashMap<>();

        String queryStr =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                        "SELECT ?State ?StateShape ?StateNodeShape ?Mode ?pasteurOnMode ?derivative ?domainConstraint ?startCond ?endCond\n" +
                        "WHERE{\n" +
                        "   ?State a pre:State;\n" +
                        "         pre:hasPasteurizer  pre:Pasteurizer;\n" +
                        "         pre:hasShape   ?StateShape.\n" +
                        "    \n" +
                        "  ?StateShape a sh:NodeShape;\n" +
                        "    sh:property [\n" +
                        "            sh:node ?StateNodeShape;\n" +
                        "            sh:path pre:hasPasteurizer\n" +
                        "    ].\n" +
                        "    \n" +
                        "  ?StateNodeShape  a  sh:NodeShape;\n" +
                        "            sh:property[\n" +
                        "            sh:hasValue ?Mode\n" +
                        "   ].\n" +
                        " \n" +
                        "    \n" +
                        "  ?pasteurOnMode a pre:ModeRecord ;\n" +
                        "                 pre:hasDevice  pre:Pasteurizer;\n" +
                        "                 pre:hasMode  ?Mode;\n" +
                        "                 pre:hasODE ?odeBlank .\n" +
                        "    \n" +
                        "  ?odeBlank rdf:type                       pre:ODE ;   \n" +
                        "            pre:derivative                 ?derivative ;\n" +
                        "            pre:startingCondition          ?startCond ;\n" +
                        "            pre:endingCondition            ?endCond ;\n" +
                        "            pre:evolutionDomainConstraint  ?domainConstraint .\n" +
                        "}";

        System.out.println("Getting states with pasteurization modes...");

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                String stateUri = solution.get("State").toString();
                String modeUri = solution.get("Mode").toString();
                String pasteurModeUri = solution.get("pasteurOnMode").toString();
                String derivative = solution.get("derivative").toString();
                String domainConstraint = solution.get("domainConstraint").toString();
                String startingCondition = solution.get("startCond").toString();
                String endingCondition = solution.get("endCond").toString();

                PasteurizationModeInfo modeInfo = new PasteurizationModeInfo(
                        stateUri, modeUri, pasteurModeUri, derivative,
                        domainConstraint, startingCondition, endingCondition
                );

                stateModesMap.put(stateUri, modeInfo);

                System.out.println("Found state: " + stateUri);
                System.out.println("  mode: " + modeUri);
                System.out.println("  pasteurization mode: " + pasteurModeUri);
                System.out.println("  derivative: " + derivative);
                System.out.println("  domain: " + domainConstraint);
                System.out.println("  starting condition: " + startingCondition);
            }
        } catch (Exception e) {
            System.err.println("Error getting states with pasteurization modes: " + e.getMessage());
        }

        return stateModesMap;
    }

    /**
     * Build the first KeymaeraX proof request
     */
    private String buildFirstProofRequest(String xRange, String startingCondition) {
        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n");
        sb.append("Real x;\n");
        sb.append("End.\nProblem\n");
        sb.append("(").append(xRange).append(") -> (").append(startingCondition).append(")\n");
        sb.append("End.\nEnd.");
        return sb.toString();
    }

    /**
     * Build the second KeymaeraX proof request
     */
    private String buildSecondProofRequest(String s322XRange, String ode, String domainConstraint, String s2XRange) {
        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n");
        sb.append("Real x;\n");
        sb.append("End.\nProblem\n");
        sb.append("(").append(s322XRange).append(") -> [{")
                .append(ode).append(" & ").append(domainConstraint)
                .append(" & ((").append(s322XRange).append(") | (").append(s2XRange).append("))")
                .append("}] (").append(s2XRange).append(")\n");
        sb.append("End.\nEnd.");
        return sb.toString();
    }

    /**
     * Class to store information about a state's pasteurization mode
     */
    private static class PasteurizationModeInfo {
        String stateUri;         // The state that has this mode
        String modeUri;          // The basic mode (OnMode or OffMode)
        String pasteurModeUri;   // The full pasteurization mode URI
        String derivative;
        String domainConstraint;
        String startingCondition;
        String endingCondition;

        public PasteurizationModeInfo(String stateUri, String modeUri, String pasteurModeUri,
                                      String derivative, String domainConstraint,
                                      String startingCondition, String endingCondition) {
            this.stateUri = stateUri;
            this.modeUri = modeUri;
            this.pasteurModeUri = pasteurModeUri;
            this.derivative = derivative;
            this.domainConstraint = domainConstraint;
            this.startingCondition = startingCondition;
            this.endingCondition = endingCondition;
        }
    }
}
