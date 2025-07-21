package org.example;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

import java.util.*;

/**
 * Custom builtin "isNext".
 *   1) forbid self-loop
 *   2) pull device modes + ODE (SPARQL #1)
 *   3) pull numeric constraints for h1/h2 (SPARQL #2)
 *   4) if modes differ -> false
 *   5) else build KeymaeraX formula, call prover, invert result
 */
public class IsNextBuiltin extends BaseBuiltin {

    private final Dataset dataset;
    private static int callCount = 0;  // Track number of calls

    public IsNextBuiltin(Dataset ds) {
        this.dataset = ds;
        System.out.println("=== IsNextBuiltin initialized ===");
    }

    @Override public String getName()     { return "isNext"; }
    @Override public int    getArgLength(){ return 2; }

    /* ============================================================ */
    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext ctx){
        callCount++;
        System.out.println("\n=== IsNextBuiltin bodyCall #" + callCount + " ===");

        if (args[0].isURI() && args[1].isURI()) {
            String s1 = args[0].getURI();
            String s2 = args[1].getURI();

            System.out.println("Checking: " + extractLocalName(s1) + " -> " + extractLocalName(s2));

            if (s1.equals(s2)) {
                System.out.println("RESULT: false (self-loop)");
                return false;
            }

            StateInfo st1 = gatherStateInfo(s1);
            StateInfo st2 = gatherStateInfo(s2);

            if (st1 == null || st2 == null) {
                System.out.println("RESULT: false (null state info)");
                return false;
            }

            // Print state details
            System.out.println("\nState 1 (" + extractLocalName(s1) + "):");
            printStateInfo(st1);
            System.out.println("\nState 2 (" + extractLocalName(s2) + "):");
            printStateInfo(st2);

            if (!checkSameMode(st1, st2)) {
                System.out.println("\nMODE CHECK: Different modes");
                System.out.println("RESULT: false (different modes)");
                return false;
            }

            System.out.println("\nMODE CHECK: Same modes - proceeding to KeYmaera X");

            String formula = buildProofFormula(st1, st2);
            System.out.println("\n=== KeYmaera X formula ===");
            System.out.println(formula);
            System.out.println("=== End formula ===");

            /* ---------- Call KeYmaera X ---------- */
            System.out.println("\nCalling KeYmaera X prover...");
            String res = Prove_helper.prove(formula);
            System.out.println("KeYmaera X returned: " + res);

            boolean result = "false".equals(res);
            System.out.println("FINAL RESULT: " + result + " (isNext relationship exists: " + result + ")");

            if (result) {
                System.out.println(">>> CREATING TRIPLE: " + extractLocalName(s1) + " next " + extractLocalName(s2));
            }

            return result;
        }

        System.out.println("RESULT: false (non-URI arguments)");
        return false;
    }

    private String extractLocalName(String uri) {
        return uri.substring(uri.lastIndexOf('#') + 1);
    }

    private void printStateInfo(StateInfo state) {
        System.out.println("  Modes:");
        for (ModeRow mode : state.modes) {
            System.out.println("    Device: " + extractLocalName(mode.deviceUri) +
                    ", Mode: " + extractLocalName(mode.modeUri));
            if (!mode.der.isBlank()) {
                System.out.println("      Derivative: " + mode.der);
            }
            if (!mode.dom.isBlank()) {
                System.out.println("      Domain: " + mode.dom);
            }
        }
        System.out.println("  Constraints:");
        for (ShapeRow shape : state.shapes) {
            System.out.println("    " + shape.getLocalName() + " " + shape.op + " " + shape.val);
        }
    }

    /* ============================================================
     *                    SPARQL #1  (Mode & ODE)
       ============================================================ */
    private List<ModeRow> queryModesAndODEs(String stateUri){
        String sparql = String.format(
                "PREFIX sh:<http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre:<http://anonymous.example.org#>\n" +
                        "SELECT DISTINCT ?Device ?deviceMode ?Derivative ?EvolutionDomainConstraint ?deviceModeRecord\n" +
                        "WHERE {\n" +
                        "  <%s> rdf:type pre:State ; pre:hasShape ?Shape ; ?devicePred ?Device .\n" +
                        "  ?Shape sh:property [ sh:path ?devicePred ; sh:node ?DevShape ] .\n" +
                        "  ?DevShape sh:property [ sh:path pre:mode ; sh:hasValue ?deviceMode ] .\n" +
                        "  OPTIONAL {\n" +
                        "    ?realRec a pre:ModeRecord ;\n" +
                        "             pre:hasDevice ?Device ;\n" +
                        "             pre:hasMode   ?deviceMode ;\n" +
                        "             pre:hasODE [ rdf:type pre:ODE ;\n" +
                        "                          pre:derivative ?realDer ;\n" +
                        "                          pre:evolutionDomainConstraint ?realDom ] .\n" +
                        "  }\n" +
                        "  BIND(COALESCE(?realRec, IRI(CONCAT(\"urn:dummy#\",STRUUID()))) AS ?deviceModeRecord)\n" +
                        "  BIND(COALESCE(?realDer,  \"\") AS ?Derivative)\n" +
                        "  BIND(COALESCE(?realDom,  \"\") AS ?EvolutionDomainConstraint)\n" +
                        "}", stateUri);

        List<ModeRow> out = new ArrayList<>();
        Model m = dataset.getDefaultModel();

        System.out.println("\nExecuting SPARQL query for modes/ODEs...");

        try (QueryExecution qx = QueryExecutionFactory.create(sparql, m)){
            ResultSet results = qx.execSelect();
            int count = 0;
            while (results.hasNext()) {
                QuerySolution sol = results.next();
                count++;
                out.add(new ModeRow(
                        sol.getResource("Device").getURI(),
                        sol.getResource("deviceMode").getURI(),
                        sol.getLiteral("Derivative").getString(),
                        sol.getLiteral("EvolutionDomainConstraint").getString(),
                        sol.getResource("deviceModeRecord").getURI()
                ));
            }
            System.out.println("Found " + count + " mode records");
        }
        return out;
    }

    /* ============================================================
     *                    SPARQL #2  (h1 / h2 constraints)
       ============================================================ */
    private List<ShapeRow> queryNumericRanges(String stateUri){
        String sparql = String.format(
                "PREFIX sh:<http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre:<http://anonymous.example.org#>\n" +
                        "SELECT DISTINCT ?var ?op ?val\n" +
                        "WHERE {\n" +
                        "  <%s> pre:hasShape ?Shape .\n" +
                        "  ?Shape sh:property [ sh:node ?DevShape ] .\n" +
                        "  ?DevShape sh:property ?P .\n" +
                        "  ?P sh:path ?var .\n" +
                        "  FILTER(?var IN (pre:h1, pre:h2))\n" +
                        "  {\n" +
                        "    ?P sh:hasValue ?v .       BIND(\"=\" AS ?op)\n" +
                        "  } UNION {\n" +
                        "    ?P sh:minExclusive ?v .   BIND(\">\" AS ?op)\n" +
                        "  } UNION {\n" +
                        "    ?P sh:maxExclusive ?v .   BIND(\"<\" AS ?op)\n" +
                        "  } UNION {\n" +
                        "    ?P sh:minInclusive ?v .   BIND(\">=\" AS ?op)\n" +
                        "  } UNION {\n" +
                        "    ?P sh:maxInclusive ?v .   BIND(\"<=\" AS ?op)\n" +
                        "  }\n" +
                        "  BIND(STR(?v) AS ?val)\n" +
                        "} ORDER BY ?var ?op", stateUri);

        List<ShapeRow> out = new ArrayList<>();
        Model m = dataset.getDefaultModel();

        System.out.println("Executing SPARQL query for numeric constraints...");

        try(QueryExecution qx = QueryExecutionFactory.create(sparql, m)){
            ResultSet results = qx.execSelect();
            int count = 0;
            while (results.hasNext()) {
                QuerySolution sol = results.next();
                count++;
                out.add(new ShapeRow(
                        sol.getResource("var").getURI(),
                        sol.getLiteral("op").getString(),
                        sol.getLiteral("val").getString()
                ));
            }
            System.out.println("Found " + count + " numeric constraints");
        }
        return out;
    }

    /* ============================================================ */
    private StateInfo gatherStateInfo(String uri){
        System.out.println("\nGathering info for state: " + extractLocalName(uri));
        StateInfo s = new StateInfo(uri);
        s.modes.addAll(queryModesAndODEs(uri));
        s.shapes.addAll(queryNumericRanges(uri));
        return s;
    }

    private boolean checkSameMode(StateInfo a, StateInfo b){
        Map<String,String> m1 = new HashMap<>(), m2 = new HashMap<>();
        a.modes.forEach(r -> m1.put(r.deviceUri, r.modeUri));
        b.modes.forEach(r -> m2.put(r.deviceUri, r.modeUri));

        System.out.println("\nComparing modes:");
        System.out.println("State 1 modes: " + m1);
        System.out.println("State 2 modes: " + m2);

        return m1.equals(m2);
    }

    /* ============================================================
     *            Build KeymaeraX formula (with de‑dup)
       ============================================================ */
    private String buildProofFormula(StateInfo s1, StateInfo s2){
        System.out.println("\nBuilding proof formula...");


        Collection<String> c1   = s1.buildShapeConstraints();
        Collection<String> c2   = s2.buildShapeConstraints();
        Collection<String> odes = s1.buildOdeExpressions();
        Collection<String> doms = s1.buildDomainExpressions();

        System.out.println("State 1 constraints: " + c1);
        System.out.println("State 2 constraints: " + c2);
        System.out.println("ODEs: " + odes);
        System.out.println("Domain constraints: " + doms);

        LinkedHashSet<String> vars = new LinkedHashSet<>();
        s1.shapes.forEach(r -> vars.add(r.getLocalName()));
        String varDecl = vars.isEmpty() ? ""
                : "  Real " + String.join(", ", vars) + ";";

        StringBuilder sb = new StringBuilder();
        sb.append("ArchiveEntry \"").append(UUID.randomUUID()).append("\"\n");
        sb.append("ProgramVariables\n").append(varDecl).append("\nEnd.\nProblem\n");

        sb.append("(").append(joinWithAnd(c1)).append(")\n");
        sb.append("-> [{ ");
        if (!odes.isEmpty()) sb.append(joinWithComma(odes)).append(" & ");
        sb.append(joinWithAnd(doms));
        sb.append(" & ((").append(joinWithAnd(c1))
                .append(")|(").append(joinWithAnd(c2)).append("))");
        sb.append(" }] (").append(joinWithAnd(c1)).append(")\nEnd.\nEnd.\n");
        return sb.toString();
    }

    /* ------------------------------ util (de‑dup) --------- */
    private static String joinWithAnd(Collection<String> xs){
        return xs.isEmpty() ? "true"
                : String.join(" & ", new LinkedHashSet<>(xs));
    }

    private static String joinWithComma(Collection<String> xs){
        return xs.isEmpty() ? ""
                : String.join(", ", new LinkedHashSet<>(xs));
    }

    /* ============================================================ */
    /*                      Data classes                            */
    /* ============================================================ */
    static class StateInfo{
        final String uri;
        final List<ModeRow>  modes  = new ArrayList<>();
        final List<ShapeRow> shapes = new ArrayList<>();
        StateInfo(String u){ uri = u; }

        Collection<String> buildShapeConstraints(){
            LinkedHashSet<String> set = new LinkedHashSet<>();
            shapes.forEach(r -> set.add(r.getLocalName() + r.op + r.val));
            return set;
        }

        Collection<String> buildOdeExpressions(){
            LinkedHashSet<String> set = new LinkedHashSet<>();
            modes.forEach(m -> { if(!m.der.isBlank()) set.add(m.der); });
            return set;
        }

        Collection<String> buildDomainExpressions(){
            LinkedHashSet<String> set = new LinkedHashSet<>();
            modes.forEach(m -> { if(!m.dom.isBlank()) set.add(m.dom); });
            return set;
        }
    }

    static class ModeRow{
        final String deviceUri, modeUri, der, dom, rec;
        ModeRow(String d, String m, String de, String dn, String r){
            deviceUri = d; modeUri = m; der = de; dom = dn; rec = r;
        }
    }

    static class ShapeRow{
        final String varUri, op, val;
        ShapeRow(String v, String o, String val){
            this.varUri = v;
            this.op = o;
            this.val = val;
        }
        String getLocalName(){
            return varUri.substring(varUri.indexOf('#') + 1);
        }
    }
}