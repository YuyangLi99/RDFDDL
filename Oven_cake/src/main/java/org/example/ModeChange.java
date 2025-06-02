package org.example;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

import java.util.*;

/**
 * A custom Builtin "mode_change(?s1, ?s2)" that implements:
 *  (1) s1 != s2
 *  (2) s1, s2 share the same numeric variable constraints (like x>=55, x<=65)
 *  (3) Then check two KeymaeraX formulas:
 *       a) (s1Constraints) -> (mode1.startingCondition)
 *          -- KeymaeraX "!false" => interpret success
 *       b) (mode1.endingCondition) <-> (mode2.startingCondition)
 *          -- KeymaeraX "true" => interpret success
 *      If both pass => we conclude "mode_change(s1, s2) = true".
 *
 * Now with extra debug prints to observe:
 *   - The constraints for each state
 *   - The modes (startCond, endCond)
 *   - The KeymaeraX formulas and their results
 */
public class ModeChange extends BaseBuiltin {

    private final Dataset dataset;

    public ModeChange(Dataset ds) {
        this.dataset = ds;
    }

    @Override
    public String getName() {
        return "mode_change";
    }

    @Override
    public int getArgLength() {
        return 2;
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        if(args[0].isURI() && args[1].isURI()) {
            String s1Uri = args[0].getURI();
            String s2Uri = args[1].getURI();

            // 1) exclude self-loop
            if(s1Uri.equals(s2Uri)) {
                System.out.println("[ModeChangeBuiltin] Skip self-loop => false");
                return false;
            }

            // 2) gather shape constraints => must match
            List<String> s1Constraints = gatherShapeConstraints(s1Uri);
            List<String> s2Constraints = gatherShapeConstraints(s2Uri);

            System.out.println("[ModeChangeBuiltin] s1 = " + s1Uri + " => constraints : " + s1Constraints);
            System.out.println("[ModeChangeBuiltin] s2 = " + s2Uri + " => constraints : " + s2Constraints);

            if(! sameConstraintSet(s1Constraints, s2Constraints)) {
                System.out.println("[ModeChangeBuiltin] s1, s2 constraints differ => false");
                return false;
            }

            // 3) gather s1, s2 modes
            List<ModeCondRow> s1Modes = gatherModeConditions(s1Uri);
            List<ModeCondRow> s2Modes = gatherModeConditions(s2Uri);

            System.out.println("[ModeChangeBuiltin] s1 => modes: ");
            for(ModeCondRow m : s1Modes) {
                System.out.println("  modeUri=" + m.modeUri + ", start=" + m.startingCondition + ", end=" + m.endingCondition);
            }
            System.out.println("[ModeChangeBuiltin] s2 => modes: ");
            for(ModeCondRow m : s2Modes) {
                System.out.println("  modeUri=" + m.modeUri + ", start=" + m.startingCondition + ", end=" + m.endingCondition);
            }

            // 4) for each mode1 in s1
            for(ModeCondRow m1 : s1Modes) {
                // build formula #1 => (s1Constraints) -> (m1.startingCondition)
                String formula1 = buildImplicationFormula(s1Constraints, m1.startingCondition);
                System.out.println("[ModeChangeBuiltin] Checking formula1:\n" + formula1);

                String result1 = Prove_helper.prove(formula1);
                System.out.println("  => KeymaeraX result1 = " + result1 + " (interpret 'false'=success for ->)");

                // interpret "false" as success for implication
                if(! "false".equals(result1)) {
                    // means didn't succeed => skip this mode1
                    System.out.println("  => Not success => skip mode1=" + m1.modeUri);
                    continue;
                }

                // next => check (m1.endingCondition) <-> (some mode2.startingCondition)
                if(m1.endingCondition==null || m1.endingCondition.isEmpty()) {
                    // no ending => skip
                    System.out.println("  => mode1 has empty endingCondition => skip");
                    continue;
                }

                for(ModeCondRow m2 : s2Modes) {
                    // skip if same modeUri => we want mode changed
                    if(m1.modeUri.equals(m2.modeUri)) {
                        continue;
                    }
                    if(m2.startingCondition==null || m2.startingCondition.isEmpty()) {
                        continue;
                    }

                    String formula2 = buildEquivalenceFormula(m1.endingCondition, m2.startingCondition);
                    System.out.println("[ModeChangeBuiltin] Checking formula2:\n" + formula2);

                    String result2 = Prove_helper.prove(formula2);
                    System.out.println("  => KeymaeraX result2 = " + result2 + " (interpret 'true'=success for <->)");

                    // interpret "true" as success for <->
                    if("true".equals(result2)) {
                        // success => return true => builtin => produce (s1 :ModeChange s2)
                        System.out.println("[ModeChangeBuiltin] => success => (s1, s2) = (" + s1Uri + ", " + s2Uri + ")");
                        return true;
                    }
                    System.out.println("  => Not success => try next mode2");
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------
    // Query => gather shape constraints => e.g. [ "x>=180", "x<=180" ]
    // Updated to use the new SPARQL query provided
    // -------------------------------------------------------------------
    private List<String> gatherShapeConstraints(String stateUri) {
        String sparql =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n"+
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
                        "PREFIX pre: <http://anonymous.example.org#>\n"+
                        "\n"+
                        "SELECT DISTINCT ?State ?StateShape ?StateDeviceShape ?variable ?MAX ?MIN\n"+
                        "WHERE {\n"+
                        "  BIND(<"+ stateUri +"> as ?State)\n"+
                        "  ?State rdf:type pre:State ;\n"+
                        "         pre:hasShape ?StateShape .\n"+
                        "  ?StateShape rdf:type sh:NodeShape ;\n"+
                        "              sh:property [\n"+
                        "                sh:node ?StateDeviceShape\n"+
                        "              ] .\n"+
                        "  ?StateDeviceShape rdf:type sh:NodeShape ;\n"+
                        "                    sh:property ?subProp .\n"+
                        "  ?subProp sh:path ?variable .\n"+
                        "  FILTER (?variable IN (pre:x))\n"+
                        "  OPTIONAL { ?subProp sh:maxInclusive ?MAX }\n"+
                        "  OPTIONAL { ?subProp sh:minInclusive ?MIN }\n"+
                        "}\n"+
                        "ORDER BY ?State ?StateShape ?StateDeviceShape ?variable";

        List<String> constraints = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try(QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while(rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String varUri = sol.getResource("variable").getURI();
                String varName = varUri.endsWith("#x")? "x" : "P";

                String maxVal = (sol.contains("MAX"))? lexicalOf(sol.get("MAX")) : null;
                String minVal = (sol.contains("MIN"))? lexicalOf(sol.get("MIN")) : null;

                if(minVal != null && !minVal.isEmpty()) {
                    constraints.add(varName + ">=" + minVal);
                }
                if(maxVal != null && !maxVal.isEmpty()) {
                    constraints.add(varName + "<=" + maxVal);
                }
            }
        }
        return constraints;
    }

    // -------------------------------------------------------------------
    // Query => gather each mode => "modeUri", "startingCondition", "endingCondition"
    // Updated to use the new SPARQL query provided (focusing on Oven devices)
    // -------------------------------------------------------------------
    private List<ModeCondRow> gatherModeConditions(String stateUri) {
        String sparql =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n"+
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
                        "PREFIX pre: <http://anonymous.example.org#>\n"+
                        "\n"+
                        "SELECT DISTINCT ?state ?StateShape ?deviceMode ?endingCondition ?startingCondition ?deviceModeRecord\n"+
                        "WHERE {\n"+
                        "  BIND(<"+ stateUri +"> as ?state)\n"+
                        "  {\n"+
                        "    ?state pre:hasOven ?Device .\n"+
                        "    ?StateShape sh:property [\n"+
                        "      sh:path pre:hasOven ;\n"+
                        "      sh:node ?deviceShape\n"+
                        "    ] .\n"+
                        "  }\n"+
                        "  ?state rdf:type pre:State ;\n"+
                        "         pre:hasShape ?StateShape .\n"+
                        "  ?StateShape a sh:NodeShape .\n"+
                        "  ?deviceShape a sh:NodeShape ;\n"+
                        "               sh:property [ sh:path pre:mode ; sh:hasValue ?deviceMode ] .\n"+
                        "  \n"+
                        "  ?deviceModeRecord a pre:ModeRecord ;\n"+
                        "                    pre:hasDevice ?Device ;\n"+
                        "                    pre:hasMode ?deviceMode ;\n"+
                        "                    pre:hasODE [\n"+
                        "                      rdf:type pre:ODE;\n"+
                        "                      pre:endingCondition ?endingCondition;\n"+
                        "                      pre:startingCondition ?startingCondition\n"+
                        "                    ] .\n"+
                        "}\n"+
                        "ORDER BY ?state ?Device";

        List<ModeCondRow> result = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try(QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while(rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String modeUri   = sol.getResource("deviceMode").getURI();
                String sc        = lexicalOf(sol.get("startingCondition"));
                String ec        = lexicalOf(sol.get("endingCondition"));
                String rec       = sol.contains("deviceModeRecord")? sol.getResource("deviceModeRecord").getURI() : null;

                result.add(new ModeCondRow(modeUri, sc, ec, rec));
            }
        }
        return result;
    }

    // -------------------------------------------------------------------
    // helper:  strip "^^xsd:long" etc => only keep lexical
    // -------------------------------------------------------------------
    private String lexicalOf(RDFNode node) {
        if(node == null) return "";
        if(node.isLiteral()) {
            return node.asLiteral().getLexicalForm();
        }
        return node.toString();
    }

    // -------------------------------------------------------------------
    // compare sets ignoring order => must be identical
    // e.g. c1=[x>=180], c2=[x>=180]
    // -------------------------------------------------------------------
    private boolean sameConstraintSet(List<String> c1, List<String> c2) {
        if(c1.size()!=c2.size()) return false;
        Set<String> set1 = new HashSet<>(c1);
        Set<String> set2 = new HashSet<>(c2);
        return set1.equals(set2);
    }

    // -------------------------------------------------------------------
    // build formula #1 => (s1Constraints) -> (mode1.startingCondition)
    // KeymaeraX: interpret "false" as success
    // Updated to use only x variable
    // -------------------------------------------------------------------
    private String buildImplicationFormula(List<String> ante, String cons) {
        String antecedent = String.join(" & ", ante);
        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n");
        sb.append("  Real x;\n");
        sb.append("End.\nProblem\n");
        sb.append("(").append(antecedent).append(") -> (").append(cons).append(")\n");
        sb.append("End.\nEnd.\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------
    // build formula #2 => (mode1.endingCondition) <-> (mode2.startingCondition)
    // KeymaeraX: interpret "true" as success
    // Updated to use only x variable
    // -------------------------------------------------------------------
    private String buildEquivalenceFormula(String left, String right) {
        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n");
        sb.append("  Real x;\n");
        sb.append("End.\nProblem\n");
        sb.append("(").append(left).append(") <-> (").append(right).append(")\n");
        sb.append("End.\nEnd.\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------
    // container for a single "mode" => sc, ec
    // -------------------------------------------------------------------
    static class ModeCondRow {
        String modeUri;
        String startingCondition;
        String endingCondition;
        String modeRecordUri;

        ModeCondRow(String mu, String sc, String ec, String rec) {
            this.modeUri   = mu;
            this.startingCondition = sc;
            this.endingCondition  = ec;
            this.modeRecordUri    = rec;
        }
    }
}