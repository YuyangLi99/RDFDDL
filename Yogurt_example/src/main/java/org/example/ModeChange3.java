package org.example;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

import java.util.*;

public class ModeChange3 extends BaseBuiltin {

    private Dataset dataset;

    public ModeChange3(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public String getName() {
        return "mode_change3";
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

        // Only when it starts at s522, we go to the next steps
        if (!"https://anonymous.example.org#s522".equals(s1Uri)) {
            System.out.println("Source state is not s522, skipping");
            return false;
        }

        // 1. Extract constraints from s522
        Map<Double, Double> s522XRange = extractStateXRange(s1Uri);
        if (s522XRange == null) {
            System.out.println("Failed to extract x range from s522");
            return false;
        }

        double s522Min = s522XRange.keySet().iterator().next();
        double s522Max = s522XRange.values().iterator().next();
        String s522XRangeStr = "x >= " + s522Min + " & x <= " + s522Max;
        System.out.println("s522 x range: " + s522XRangeStr);

        // 2. Check if s522 has PasteurizationOffMode
        boolean hasPasteurOffMode = checkStateHasPasteurOffMode(s1Uri);
        if (!hasPasteurOffMode) {
            System.out.println("s522 does not have PasteurizationOffMode");
            return false;
        }

        // 3. Get all states with cooler modes
        Map<String, CoolerModeInfo> stateModesMap = getStatesCoolerModes();
        if (stateModesMap.isEmpty()) {
            System.out.println("No states with cooler modes found");
            return false;
        }

        // Check if destination state has a cooler mode
        if (!stateModesMap.containsKey(s2Uri)) {
            System.out.println("Destination state does not have a cooler mode");
            return false;
        }

        // 4. Get all unique cooler modes for testing
        List<CoolerModeInfo> uniqueModes = new ArrayList<>();
        Set<String> addedModes = new HashSet<>();

        for (CoolerModeInfo info : stateModesMap.values()) {
            if (!addedModes.contains(info.coolerModeUri)) {
                uniqueModes.add(info);
                addedModes.add(info.coolerModeUri);
            }
        }

        // 5. Find a mode that passes the first proof
        CoolerModeInfo chosenMode = null;

        for (CoolerModeInfo mode : uniqueModes) {
            System.out.println("Testing mode: " + mode.coolerModeUri);

            String proof1 = buildFirstProofRequest(s522XRangeStr, mode.startingCondition);
            System.out.println("KeymaeraX First Proof Request:");
            System.out.println(proof1);

            String result = Prove_helper.prove(proof1);
            System.out.println("Proof result: " + result);

            if ("true".equals(result)) {
                chosenMode = mode;
                System.out.println("Selected mode: " + mode.coolerModeUri);
                break;
            }
        }

        if (chosenMode == null) {
            System.out.println("No suitable mode found");
            return false;
        }

        // 6. Check if destination state has the compatible mode
        CoolerModeInfo s2ModeInfo = stateModesMap.get(s2Uri);
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
        String proof2 = buildSecondProofRequest(s522XRangeStr, ode, chosenMode.domainConstraint, s2XRangeStr);
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
                        "SELECT ?variable ?minVal ?maxVal WHERE {\n" +
                        "  BIND(<" + stateUri + "> AS ?s1)\n" +
                        "  ?s1 pre:hasShape ?s1Shape .\n" +
                        "  ?s1Shape sh:property ?prop .\n" +
                        "  ?prop sh:node ?deviceSubShape .\n" +
                        "  ?deviceSubShape sh:property ?propNode .\n" +
                        "  ?propNode sh:path ?variable .\n" +
                        "  FILTER (?variable IN (pre:x, pre:P))\n" +
                        "  OPTIONAL { ?propNode sh:minInclusive ?minVal }\n" +
                        "  OPTIONAL { ?propNode sh:maxInclusive ?maxVal }\n" +
                        "}";

        System.out.println("Executing SPARQL query for state x range...");

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();
            Map<Double, Double> range = new HashMap<>();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                String variable = solution.get("variable").toString();
                if (!variable.equals("https://anonymous.example.org#x")) {
                    continue;
                }

                // Get min and max values
                double minVal = solution.get("minVal") != null ?
                        solution.get("minVal").asLiteral().getDouble() : Double.NEGATIVE_INFINITY;
                double maxVal = solution.get("maxVal") != null ?
                        solution.get("maxVal").asLiteral().getDouble() : Double.POSITIVE_INFINITY;

                range.put(minVal, maxVal);
                System.out.println("Found x range for " + stateUri + ": " + minVal + " to " + maxVal);
                return range; // Return as soon as we find x range
            }

            System.out.println("No x range found for " + stateUri);
            return null;
        } catch (Exception e) {
            System.err.println("Error querying state x range: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if state has PasteurizationOffMode
     */
    private boolean checkStateHasPasteurOffMode(String stateUri) {
        String queryStr =
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "ASK {\n" +
                        "  <" + stateUri + "> pre:hasPasteurizer pre:Pasteurizer .\n" +
                        "  <" + stateUri + "> pre:hasShape ?shape .\n" +
                        "  ?shape sh:property ?prop .\n" +
                        "  ?prop sh:path pre:hasPasteurizer .\n" +
                        "  ?prop sh:node ?pasteurShape .\n" +
                        "  ?pasteurShape sh:property ?modeProp .\n" +
                        "  ?modeProp sh:path pre:mode .\n" +
                        "  ?modeProp sh:hasValue pre:OffMode .\n" +
                        "}";

        System.out.println("Checking if state has PasteurizationOffMode...");

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            boolean result = qexec.execAsk();
            System.out.println("State " + stateUri + " has PasteurizationOffMode: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("Error checking PasteurizationOffMode: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get information about states with cooler modes
     * @return Map of state URIs to their compatible cooler modes
     */
    private Map<String, CoolerModeInfo> getStatesCoolerModes() {
        Map<String, CoolerModeInfo> stateModesMap = new HashMap<>();

        String queryStr =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                        "SELECT ?State ?StateShape ?StateNodeShape ?Mode ?CoolerMode ?derivative ?domainConstraint ?startCond ?endCond\n" +
                        "WHERE{\n" +
                        "  ?State a pre:State;\n" +
                        "         pre:hasCooler  pre:Cooler;\n" +
                        "         pre:hasShape   ?StateShape.\n" +
                        "    \n" +
                        "  ?StateShape a sh:NodeShape;\n" +
                        "    sh:property [\n" +
                        "            sh:node ?StateNodeShape;\n" +
                        "            sh:path pre:hasCooler\n" +
                        "    ].\n" +
                        "    \n" +
                        "  ?StateNodeShape  a  sh:NodeShape;\n" +
                        "            sh:property[\n" +
                        "            sh:hasValue ?Mode\n" +
                        "   ].\n" +
                        "    \n" +
                        "  ?CoolerMode a pre:ModeRecord ;\n" +
                        "                 pre:hasDevice  pre:Cooler;\n" +
                        "                 pre:hasMode  ?Mode;\n" +
                        "                 pre:hasODE ?odeBlank .\n" +
                        "  ?odeBlank rdf:type                       pre:ODE ;   \n" +
                        "            pre:derivative                 ?derivative ;\n" +
                        "            pre:startingCondition          ?startCond ;\n" +
                        "            pre:endingCondition            ?endCond ;\n" +
                        "            pre:evolutionDomainConstraint  ?domainConstraint .\n" +
                        "}";

        System.out.println("Getting states with cooler modes...");

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                String stateUri = solution.get("State").toString();
                String modeUri = solution.get("Mode").toString();
                String coolerModeUri = solution.get("CoolerMode").toString();
                String derivative = solution.get("derivative").toString();
                String domainConstraint = solution.get("domainConstraint").toString();
                String startingCondition = solution.get("startCond").toString();
                String endingCondition = solution.get("endCond").toString();

                CoolerModeInfo modeInfo = new CoolerModeInfo(
                        stateUri, modeUri, coolerModeUri, derivative,
                        domainConstraint, startingCondition, endingCondition
                );

                stateModesMap.put(stateUri, modeInfo);

                System.out.println("Found state with cooler: " + stateUri);
                System.out.println("  mode: " + modeUri);
                System.out.println("  cooler mode: " + coolerModeUri);
                System.out.println("  derivative: " + derivative);
                System.out.println("  domain: " + domainConstraint);
                System.out.println("  starting condition: " + startingCondition);
            }
        } catch (Exception e) {
            System.err.println("Error getting states with cooler modes: " + e.getMessage());
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
    private String buildSecondProofRequest(String s522XRange, String ode, String domainConstraint, String s2XRange) {
        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n");
        sb.append("Real x;\n");
        sb.append("End.\nProblem\n");
        sb.append("(").append(s522XRange).append(") -> [{")
                .append(ode).append(" & ").append(domainConstraint)
                .append(" & ((").append(s522XRange).append(") | (").append(s2XRange).append("))")
                .append("}] (").append(s2XRange).append(")\n");
        sb.append("End.\nEnd.");
        return sb.toString();
    }

    /**
     * Class to store information about a state's cooler mode
     */
    private static class CoolerModeInfo {
        String stateUri;         // The state that has this mode
        String modeUri;          // The basic mode (OnMode or OffMode)
        String coolerModeUri;    // The full cooler mode URI
        String derivative;
        String domainConstraint;
        String startingCondition;
        String endingCondition;

        public CoolerModeInfo(String stateUri, String modeUri, String coolerModeUri,
                              String derivative, String domainConstraint,
                              String startingCondition, String endingCondition) {
            this.stateUri = stateUri;
            this.modeUri = modeUri;
            this.coolerModeUri = coolerModeUri;
            this.derivative = derivative;
            this.domainConstraint = domainConstraint;
            this.startingCondition = startingCondition;
            this.endingCondition = endingCondition;
        }
    }
}
