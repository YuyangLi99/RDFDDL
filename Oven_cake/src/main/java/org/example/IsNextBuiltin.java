package org.example;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

import java.util.*;

/**
 * A custom Builtin named "isNext" that:
 *   - For two states s1, s2:
 *       1) Disallow self-loop: if (s1 == s2) => return false
 *       2) Gather device modes + ODE (via first SPARQL)
 *       3) Gather numeric range for x (via second SPARQL)
 *       4) If device modes differ => return false
 *       5) Otherwise build a KeymaeraX formula => prove => if result="false" => return true, else false
 */
public class IsNextBuiltin extends BaseBuiltin {

    private final Dataset dataset;

    public IsNextBuiltin(Dataset ds) {
        this.dataset = ds;
    }

    @Override
    public String getName() {
        return "isNext";
    }

    @Override
    public int getArgLength() {
        return 2;
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        if (args[0].isURI() && args[1].isURI()) {
            String s1Uri = args[0].getURI();
            String s2Uri = args[1].getURI();

            // (1) No self-loop
            if (s1Uri.equals(s2Uri)) {
                System.out.println("[IsNextBuiltin] s1 == s2 => false");
                return false;
            }

            // (2) Gather both states
            StateInfo s1 = gatherStateInfo(s1Uri);
            StateInfo s2 = gatherStateInfo(s2Uri);
            if (s1 == null || s2 == null) return false;

            // (3) Check modes
            if (!checkSameMode(s1, s2)) return false;

            // (4) Build and prove formula
            String formula = buildProofFormula(s1, s2);
            System.out.println("\n=== [isNext] KeymaeraX formula ===\n" + formula);
            String result = Prove_helper.prove(formula);
            return "false".equals(result);
        }
        return false;
    }

    private StateInfo gatherStateInfo(String stateUri) {
        StateInfo info = new StateInfo(stateUri);
        info.modes.addAll(queryModesAndODEs(stateUri));
        info.shapes.addAll(queryNumericRanges(stateUri));
        return info;
    }

    // ==================== Query #1: device modes & ODEs ====================
    private List<ModeRow> queryModesAndODEs(String stateUri) {
        String sparql =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <http://anonymous.example.org#>\n" +
                        "SELECT DISTINCT ?state ?Device ?StateShape ?deviceMode ?Derivative ?EvolutionDomainConstraint ?deviceModeRecord\n" +
                        "WHERE {\n" +
                        "  { ?state pre:hasOven ?Device .\n" +
                        "    ?StateShape sh:property [ sh:path pre:hasOven; sh:node ?deviceShape ] . }\n" +
                        "  ?state rdf:type pre:State; pre:hasShape ?StateShape .\n" +
                        "  ?StateShape a sh:NodeShape .\n" +
                        "  ?deviceShape a sh:NodeShape; sh:property [ sh:path pre:mode; sh:hasValue ?deviceMode ] .\n" +
                        "  ?deviceModeRecord a pre:ModeRecord; pre:hasDevice ?Device; pre:hasMode ?deviceMode;\n" +
                        "    pre:hasODE [ rdf:type pre:ODE; pre:derivative ?Derivative; pre:evolutionDomainConstraint ?EvolutionDomainConstraint ] .\n" +
                        "  FILTER(?state = <" + stateUri + ">)\n" +
                        "}\n" +
                        "ORDER BY ?state ?Device";
        List<ModeRow> list = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                list.add(new ModeRow(
                        sol.getResource("Device").getURI(),
                        sol.getResource("deviceMode").getURI(),
                        sol.getLiteral("Derivative").getString(),
                        sol.getLiteral("EvolutionDomainConstraint").getString(),
                        sol.getResource("deviceModeRecord").getURI()
                ));
            }
        }
        return list;
    }

    // ==================== Query #2: numeric range for x only ====================
    private List<ShapeRow> queryNumericRanges(String stateUri) {
        String sparql =
                "PREFIX sh:  <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <http://anonymous.example.org#>\n" +
                        "SELECT DISTINCT ?State ?StateShape ?StateDeviceShape ?variable ?MAX ?MIN\n" +
                        "WHERE {\n" +
                        "  ?State rdf:type pre:State; pre:hasShape ?StateShape .\n" +
                        "  ?StateShape a sh:NodeShape; sh:property [ sh:node ?StateDeviceShape ] .\n" +
                        "  ?StateDeviceShape a sh:NodeShape; sh:property ?subProp .\n" +
                        "  ?subProp sh:path ?variable .\n" +
                        "  FILTER (?variable = pre:x)\n" +
                        "  OPTIONAL { ?subProp sh:maxInclusive ?MAX }\n" +
                        "  OPTIONAL { ?subProp sh:minInclusive ?MIN }\n" +
                        "  FILTER(?State = <" + stateUri + ">)\n" +
                        "}\n" +
                        "ORDER BY ?State ?StateDeviceShape";
        List<ShapeRow> list = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                list.add(new ShapeRow(
                        sol.getResource("variable").getURI(),
                        sol.contains("MAX") ? sol.getLiteral("MAX").getString() : null,
                        sol.contains("MIN") ? sol.getLiteral("MIN").getString() : null
                ));
            }
        }
        return list;
    }

    // ==================== same mode checking ====================
    private boolean checkSameMode(StateInfo s1, StateInfo s2) {
        Map<String,String> m1 = new HashMap<>();
        for (ModeRow r : s1.modes) m1.put(r.deviceUri, r.modeUri);
        Map<String,String> m2 = new HashMap<>();
        for (ModeRow r : s2.modes) m2.put(r.deviceUri, r.modeUri);
        if (!m1.keySet().equals(m2.keySet())) return false;
        for (String d : m1.keySet()) {
            if (!m1.get(d).equals(m2.get(d))) return false;
        }
        return true;
    }

    // ==================== build formula & call KeymaeraX ====================
    private String buildProofFormula(StateInfo s1, StateInfo s2) {
        List<String> c1 = s1.buildShapeConstraints();
        List<String> c2 = s2.buildShapeConstraints();
        List<String> odes = s1.buildOdeExpressions();
        List<String> doms = s1.buildDomainExpressions();

        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n  Real x;\nEnd.\nProblem\n");
        sb.append("(").append(joinWithAnd(c1)).append(")\n");
        sb.append("-> [{ ");
        if (!odes.isEmpty()) sb.append(joinWithComma(odes));
        for (String d : doms) sb.append(" & ").append(d);
        sb.append(" & ((").append(joinWithAnd(c1)).append(")|(").append(joinWithAnd(c2)).append("))");
        sb.append(" }] (").append(joinWithAnd(c1)).append(")\n");
        sb.append("End.\nEnd.\n");
        return sb.toString();
    }

    // ================ utilities ================
    private String joinWithAnd(List<String> xs) {
        return xs.isEmpty() ? "" : String.join(" & ", xs);
    }
    private String joinWithComma(List<String> xs) {
        return xs.isEmpty() ? "" : String.join(", ", xs);
    }

    // ==================== Data Classes ====================
    static class StateInfo {
        String stateUri;
        List<ModeRow> modes = new ArrayList<>();
        List<ShapeRow> shapes = new ArrayList<>();
        StateInfo(String uri) { this.stateUri = uri; }
        List<String> buildShapeConstraints() {
            List<String> res = new ArrayList<>();
            for (ShapeRow r : shapes) {
                if (r.variableUri.endsWith("#x")) {
                    if (r.minVal != null) res.add("x>=" + r.minVal);
                    if (r.maxVal != null) res.add("x<=" + r.maxVal);
                }
            }
            return res;
        }

        List<String> buildOdeExpressions() {
            List<String> res = new ArrayList<>();
            for (ModeRow m : modes) {
                if (m.derivative != null && !m.derivative.trim().isEmpty()) {
                    res.add("x' = " + m.derivative);
                }
            }
            return res;
        }


        List<String> buildDomainExpressions() {
            List<String> res = new ArrayList<>();
            for (ModeRow m : modes) res.add(m.evolutionDomainConstraint);
            return res;
        }
    }

    static class ModeRow {
        String deviceUri, modeUri, derivative, evolutionDomainConstraint, modeRecord;
        ModeRow(String dev, String mode, String deriv, String dom, String rec) {
            this.deviceUri = dev; this.modeUri = mode; this.derivative = deriv;
            this.evolutionDomainConstraint = dom; this.modeRecord = rec;
        }
    }

    static class ShapeRow {
        String variableUri, maxVal, minVal;
        ShapeRow(String var, String max, String min) {
            this.variableUri = var; this.maxVal = max; this.minVal = min;
        }
    }
}
