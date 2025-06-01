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
 *       3) Gather numeric ranges for x,P (via second SPARQL)
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

    /**
     *  The main logic of isNext(?a, ?b).
     *  If passes all checks => return true => the rule can produce ( ?a ex:next ?b ).
     */
    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        // Check both are URIs
        if (args[0].isURI() && args[1].isURI()) {
            String s1Uri = args[0].getURI();
            String s2Uri = args[1].getURI();

            // (1) Disallow self-loop
            if (s1Uri.equals(s2Uri)) {
                System.out.println("[IsNextBuiltin] s1 == s2 => return false");
                return false;
            }

            // (2) gather state info
            StateInfo s1 = gatherStateInfo(s1Uri);
            StateInfo s2 = gatherStateInfo(s2Uri);
            if (s1 == null || s2 == null) {
                return false;
            }

            // (3) check device modes => must be same for all devices
            if (!checkSameMode(s1, s2)) {
                return false;
            }

            // (4) build formula => call KeymaeraX
            String formula = buildProofFormula(s1, s2);
            System.out.println("\n=== [isNext] KeymaeraX formula ===\n" + formula);

            // (5) prove => if result="false" => interpret success => return true
            String result = Prove_helper.prove(formula);
            if("false".equals(result)) {
                // KeymaeraX proof success
                return true;
            } else {
                // KeymaeraX says "true" or other => interpret fail
                return false;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------
    // gatherStateInfo => calls 2 SPARQL queries
    // -------------------------------------------------------------------
    private StateInfo gatherStateInfo(String stateUri) {
        StateInfo info = new StateInfo(stateUri);

        // Query #1 => deviceMode + derivative/domain
        List<ModeRow> modes = queryModesAndODEs(stateUri);
        info.modes.addAll(modes);

        // Query #2 => numeric ranges (x, P => minInclusive, maxInclusive)
        List<ShapeRow> shapes = queryNumericRanges(stateUri);
        info.shapes.addAll(shapes);

        return info;
    }

    // ==================== Query #1 ====================
    private List<ModeRow> queryModesAndODEs(String stateUri) {
        // This SPARQL obtains: (device, mode, derivative, domainConstraint)
        String sparql =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "\n" +
                        "SELECT ?state ?Device ?StateShape ?deviceMode ?Derivative ?EvolutionDomainConstraint ?deviceModeRecord\n" +
                        "WHERE {\n" +
                        " {\n" +
                        "   ?state pre:hasCooler ?Device .\n" +
                        "   ?StateShape sh:property [ sh:path pre:hasCooler ; sh:node ?deviceShape ].\n" +
                        " } UNION {\n" +
                        "   ?state pre:hasHeater ?Device .\n" +
                        "   ?StateShape sh:property [ sh:path pre:hasHeater ; sh:node ?deviceShape ].\n" +
                        " } UNION {\n" +
                        "   ?state pre:hasHomogenizer ?Device .\n" +
                        "   ?StateShape sh:property [ sh:path pre:hasHomogenizer ; sh:node ?deviceShape ].\n" +
                        " } UNION {\n" +
                        "   ?state pre:hasPasteurizer ?Device .\n" +
                        "   ?StateShape sh:property [ sh:path pre:hasPasteurizer ; sh:node ?deviceShape ].\n" +
                        " }\n" +
                        " ?state rdf:type pre:State ; pre:hasShape ?StateShape .\n" +
                        " ?StateShape a sh:NodeShape .\n" +
                        " ?deviceShape a sh:NodeShape ;\n" +
                        "              sh:property [ sh:path pre:mode ; sh:hasValue ?deviceMode ] .\n" +
                        " ?deviceModeRecord a pre:ModeRecord ;\n" +
                        "                   pre:hasDevice ?Device ;\n" +
                        "                   pre:hasMode ?deviceMode ;\n" +
                        "                   pre:hasODE [\n" +
                        "                     rdf:type  pre:ODE;\n" +
                        "                     pre:derivative ?Derivative;\n" +
                        "                     pre:evolutionDomainConstraint ?EvolutionDomainConstraint\n" +
                        "                   ] .\n" +
                        " FILTER(?state = <" + stateUri + ">)\n" +
                        "}\n" +
                        "ORDER BY ?state ?Device";

        List<ModeRow> list = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try(QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while(rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String dev  = sol.getResource("Device").getURI();
                String mode = sol.getResource("deviceMode").getURI();
                String deriv= sol.getLiteral("Derivative").getString();
                String dom  = sol.getLiteral("EvolutionDomainConstraint").getString();
                String rec  = sol.getResource("deviceModeRecord").getURI();
                list.add(new ModeRow(dev, mode, deriv, dom, rec));
            }
        }
        return list;
    }

    // ==================== Query #2 ====================
    private List<ShapeRow> queryNumericRanges(String stateUri) {
        // This SPARQL obtains: shape-based numeric constraints for x, P
        String sparql =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <https://anonymous.example.org#>\n" +
                        "\n" +
                        "SELECT ?State ?StateShape ?StateDeviceShape ?variable ?MAX ?MIN\n" +
                        "WHERE {\n" +
                        "  ?State rdf:type pre:State ; pre:hasShape ?StateShape .\n" +
                        "  ?StateShape rdf:type sh:NodeShape ;\n" +
                        "              sh:property [ sh:node ?StateDeviceShape ].\n" +
                        "  ?StateDeviceShape rdf:type sh:NodeShape ;\n" +
                        "                    sh:property ?subProp .\n" +
                        "  ?subProp sh:path ?variable .\n" +
                        "  FILTER (?variable IN (pre:x, pre:P))\n" +
                        "  OPTIONAL { ?subProp sh:maxInclusive ?MAX }\n" +
                        "  OPTIONAL { ?subProp sh:minInclusive ?MIN }\n" +
                        "  FILTER(?State = <" + stateUri + ">)\n" +
                        "}\n" +
                        "ORDER BY ?State ?StateShape ?StateDeviceShape ?variable";

        List<ShapeRow> list = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try(QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while(rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String varUri   = sol.getResource("variable").getURI();
                String maxVal   = (sol.contains("MAX") && sol.get("MAX").isLiteral()) ? sol.getLiteral("MAX").getString() : null;
                String minVal   = (sol.contains("MIN") && sol.get("MIN").isLiteral()) ? sol.getLiteral("MIN").getString() : null;
                list.add(new ShapeRow(varUri, maxVal, minVal));
            }
        }
        return list;
    }

    // ==================== same mode checking ====================
    private boolean checkSameMode(StateInfo s1, StateInfo s2) {
        // build device->mode map
        Map<String,String> s1map = new HashMap<>();
        for(ModeRow row : s1.modes) {
            s1map.put(row.deviceUri, row.modeUri);
        }
        Map<String,String> s2map = new HashMap<>();
        for(ModeRow row : s2.modes) {
            s2map.put(row.deviceUri, row.modeUri);
        }

        // same device set
        if (!s1map.keySet().equals(s2map.keySet())) {
            return false;
        }
        // each device => same mode
        for(String dev : s1map.keySet()) {
            if(!s1map.get(dev).equals(s2map.get(dev))) {
                System.out.println("[isNext] device=" + dev + " => s1mode=" + s1map.get(dev)
                        + ", s2mode=" + s2map.get(dev) + ", differ => false");
                return false;
            }
        }
        return true;
    }

    // ==================== build formula & call KeymaeraX ====================
    private String buildProofFormula(StateInfo s1, StateInfo s2) {
        // gather shape constraints => s1Constraints, s2Constraints
        List<String> s1Constraints = s1.buildShapeConstraints();
        List<String> s2Constraints = s2.buildShapeConstraints();

        // gather s1 ODE => "x' = 9.68, P'=-2", domain => "x<=65, P<=20"
        List<String> s1Odes = s1.buildOdeExpressions();
        List<String> s1Domains = s1.buildDomainExpressions();

        // ex: (s1Constraints) -> [{ s1Odes & s1Domains & (s1Constraints|s2Constraints) }] (s2Constraints)
        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n");
        sb.append("  Real x;\n  Real P;\n");
        sb.append("End.\nProblem\n");
        sb.append("(").append(joinWithAnd(s1Constraints)).append(")\n-> [{ ");

        // ODE
        if(!s1Odes.isEmpty()) {
            sb.append(joinWithComma(s1Odes));
        }
        // domain
        for(String d : s1Domains) {
            sb.append(" & ").append(d);
        }

        // combine => " & ((s1Constraints)|(s2Constraints))"
        sb.append(" & ((").append(joinWithAnd(s1Constraints)).append(")|(").append(joinWithAnd(s2Constraints)).append("))");

        sb.append("}] (").append(joinWithAnd(s1Constraints)).append(")\nEnd.\nEnd.\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------
    // utility
    // -------------------------------------------------------------------
    private String joinWithAnd(List<String> xs) {
        if(xs.isEmpty()) return "";
        return String.join(" & ", xs);
    }
    private String joinWithComma(List<String> xs) {
        if(xs.isEmpty()) return "";
        return String.join(", ", xs);
    }

    // ==================== Data Classes ====================
    static class StateInfo {
        String stateUri;
        List<ModeRow> modes = new ArrayList<>();
        List<ShapeRow> shapes = new ArrayList<>();

        StateInfo(String uri) {
            this.stateUri = uri;
        }

        /** shape => produce constraints like "x<=55, P<=10" */
        List<String> buildShapeConstraints() {
            List<String> results = new ArrayList<>();
            for(ShapeRow row : shapes) {
                // if endswith #x => var="x", if #P => var="P"
                String var="";
                if(row.variableUri.endsWith("#x")) var="x";
                else if(row.variableUri.endsWith("#P")) var="P";
                if(var.isEmpty()) continue;

                if(row.minVal != null && !row.minVal.isEmpty()) {
                    results.add(var + ">=" + row.minVal);
                }
                if(row.maxVal != null && !row.maxVal.isEmpty()) {
                    results.add(var + "<=" + row.maxVal);
                }
            }
            return results;
        }

        /** modes => produce e.g. "x'=-9.68" or "P'=2" */
        List<String> buildOdeExpressions() {
            List<String> ans = new ArrayList<>();
            for(ModeRow m : modes) {
                double dv=0.0;
                try { dv=Double.parseDouble(m.derivative);} catch(Exception ex){}
                // if abs>2 => treat as x'
                if(Math.abs(dv)>2) {
                    ans.add("x' = " + m.derivative);
                } else {
                    ans.add("P' = " + m.derivative);
                }
            }
            return ans;
        }

        /** e.g. "x<=65, P<=20" from domain constraints */
        List<String> buildDomainExpressions() {
            List<String> ans = new ArrayList<>();
            for(ModeRow m : modes) {
                ans.add(m.evolutionDomainConstraint);
            }
            return ans;
        }
    }

    static class ModeRow {
        String deviceUri;
        String modeUri;
        String derivative;
        String evolutionDomainConstraint;
        String modeRecord;

        ModeRow(String dev, String mode, String deriv, String dom, String rec) {
            this.deviceUri = dev;
            this.modeUri   = mode;
            this.derivative= deriv;
            this.evolutionDomainConstraint = dom;
            this.modeRecord= rec;
        }
    }

    static class ShapeRow {
        String variableUri;
        String maxVal;
        String minVal;

        ShapeRow(String varUri, String max, String min) {
            this.variableUri= varUri;
            this.maxVal= max;
            this.minVal= min;
        }
    }
}
