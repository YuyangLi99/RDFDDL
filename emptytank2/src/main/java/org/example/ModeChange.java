package org.example;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Jena custom builtin:  mode_change(?s1, ?s2)
 * -------------------------------------------------------------
 * Rule (simplified):
 *   1) s1 ≠ s2
 *   2) Numeric constraints (h1 / h2) of the two State instances are identical.
 *   3) ∃ mode ∈ Modes(s2)  with
 *        • mode NOT in Modes(s1), and
 *        •   (StateConstraints) -> (mode.startingCondition)    proved "true" by KeYmaera X
 *      ⇒  mode_change(s1 , s2) holds.
 *
 * Implementation details
 *   • Two SPARQL queries fetch constraints and mode info (unchanged).
 *   • KeYmaera variables declared as  Real h1, h2.
 *   • endingCondition is ignored.
 *   • Extensive step‑numbered tracing via System.out; toggle with DEBUG.
 */
public class ModeChange extends BaseBuiltin {

    /* ---------------- global debug switch ---------------- */
    private static final boolean DEBUG = true;
    private static long STEP = 0;
    private static void trace(String msg) {
        if (DEBUG) System.out.println(String.format("[MC|%03d] %s", ++STEP, msg));
    }

    /* ---------------- engine setup ----------------------- */
    private final Dataset dataset;
    public ModeChange(Dataset ds) { this.dataset = ds; }

    @Override public String getName()      { return "mode_change"; }
    @Override public int    getArgLength() { return 2; }

    /* ================================================================
     * builtin core
     * ================================================================ */
    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {

        STEP = 0;                                         // reset counter per call
        if (args.length < 2 || !args[0].isURI() || !args[1].isURI()) {
            trace("Illegal arguments – expect two URIs."); return false;
        }
        String s1Uri = args[0].getURI(), s2Uri = args[1].getURI();
        trace("Invoke on   s1=" + s1Uri + "   s2=" + s2Uri);
        if (s1Uri.equals(s2Uri)) { trace("Self‑loop – abort."); return false; }

        /* ---- constraint sets ---- */
        List<String> s1Cons = gatherShapeConstraints(s1Uri);
        List<String> s2Cons = gatherShapeConstraints(s2Uri);
        trace("Constraints s1 = " + s1Cons);
        trace("Constraints s2 = " + s2Cons);
        if (!sameConstraintSet(s1Cons, s2Cons)) {
            trace("Constraint sets differ – abort."); return false;
        }

        /* ---- mode sets ---- */
        List<ModeCondRow> s1Modes = gatherModeConditions(s1Uri);
        List<ModeCondRow> s2Modes = gatherModeConditions(s2Uri);
        trace("Modes of s1 → " + s1Modes);
        trace("Modes of s2 → " + s2Modes);

        Set<String> s1ModeURIs = s1Modes.stream()
                .map(m -> m.modeUri)
                .collect(Collectors.toSet());

        /* ---- single‑implication search ---- */
        for (ModeCondRow m2 : s2Modes) {
            if (s1ModeURIs.contains(m2.modeUri)) {
                trace("Mode already active in s1 – skip " + localName(m2.modeUri));
                continue;
            }
            if (m2.startingCondition == null || m2.startingCondition.isEmpty()) {
                trace("Mode2 has empty SC – skip");
                continue;
            }

            String proof = buildImplicationFormula(s1Cons, m2.startingCondition);
            trace("Proof (new mode " + localName(m2.modeUri) + ")\n" + proof);
            String res = Prove_helper.prove(proof);
            trace("Result = " + res);
            if ("true".equals(res)) {                       // KeYmaera closed goal
                trace("SUCCESS: mode_change(" + s1Uri + ", " + s2Uri + ")");
                return true;
            }
        }
        trace("No new mode satisfies the implication.");
        return false;
    }

    /* ================================================================
     * SPARQL 1: numeric constraints (unchanged)
     * ================================================================ */
    private List<String> gatherShapeConstraints(String stateUri) {
        String sparql =
                "PREFIX sh:  <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <http://anonymous.example.org#>\n" +
                        "SELECT DISTINCT ?variable ?operator ?value WHERE {\n" +
                        "  BIND(<" + stateUri + "> AS ?State)\n" +
                        "  ?State rdf:type pre:State ; pre:hasShape ?Shape .\n" +
                        "  ?Shape sh:property [ sh:node ?DevShape ] .\n" +
                        "  ?DevShape sh:property ?Prop .\n" +
                        "  ?Prop sh:path ?variable .\n" +
                        "  FILTER (?variable IN (pre:h1, pre:h2))\n" +
                        "  { ?Prop sh:hasValue       ?v  BIND(\"=\"  AS ?operator) } UNION\n" +
                        "  { ?Prop sh:minExclusive   ?v  BIND(\">\"  AS ?operator) } UNION\n" +
                        "  { ?Prop sh:maxExclusive   ?v  BIND(\"<\"  AS ?operator) } UNION\n" +
                        "  { ?Prop sh:minInclusive   ?v  BIND(\">=\" AS ?operator) } UNION\n" +
                        "  { ?Prop sh:maxInclusive   ?v  BIND(\"<=\" AS ?operator) }\n" +
                        "  BIND(STR(?v) AS ?value)\n" +
                        "} ORDER BY ?variable ?operator";

        List<String> out = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String var = localName(sol.getResource("variable").getURI());
                String op  = sol.getLiteral("operator").getString();
                String val = sol.getLiteral("value").getString();
                out.add(var + op + val);
                trace("  ↳ constraint " + var + op + val);
            }
        }
        return out;
    }

    /* ================================================================
     * SPARQL 2: mode / SC extraction (EC ignored)
     * ================================================================ */
    private List<ModeCondRow> gatherModeConditions(String stateUri) {
        String sparql =
                "PREFIX sh:  <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <http://anonymous.example.org#>\n" +
                        "SELECT DISTINCT ?deviceMode ?startingCondition ?record WHERE {\n" +
                        "  BIND(<" + stateUri + "> AS ?state)\n" +
                        "  ?state (pre:hasTank1|pre:hasTank2) ?Device .\n" +
                        "  ?state pre:hasShape ?Shape .\n" +
                        "  ?Shape sh:property ?Outer .\n" +
                        "  ?Outer sh:path ?p ; sh:node ?DevShape .\n" +
                        "  ?state ?p ?Device .\n" +
                        "  ?DevShape sh:property [ sh:path pre:mode ; sh:hasValue ?deviceMode ] .\n" +
                        "  ?record a pre:ModeRecord ; pre:hasDevice ?Device ; pre:hasMode ?deviceMode ; pre:hasODE ?ode .\n" +
                        "  ?ode pre:startingCondition ?startingCondition .\n" +
                        "}";

        Map<String,ModeCondRow> map = new LinkedHashMap<>();
        Model m = dataset.getDefaultModel();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String mu = sol.getResource("deviceMode").getURI();
                String sc = lexicalOf(sol.get("startingCondition"));
                String rec = sol.getResource("record").getURI();
                map.putIfAbsent(mu, new ModeCondRow(mu, sc, rec));
                trace("  ↳ mode " + localName(mu) + "  SC=\"" + sc + "\"");
            }
        }
        return new ArrayList<>(map.values());
    }

    /* ================================================================
     * KeYmaera X script factory
     * ================================================================ */
    private String buildImplicationFormula(List<String> ante, String cons) {
        String ant = ante.isEmpty() ? "true" : String.join(" & ", ante);
        return "ArchiveEntry \"" + UUID.randomUUID() + "\"\n" +
                "ProgramVariables\n" +
                "  Real h1, h2;\n" +
                "End.\nProblem\n" +
                "(" + ant + ") -> (" + cons + ")\n" +
                "End.\nEnd.\n";
    }

    /* ================================================================
     * helpers
     * ================================================================ */
    private static String lexicalOf(RDFNode node) {
        if (node == null) return "";
        if (node.isLiteral()) return node.asLiteral().getLexicalForm();
        return node.toString();
    }
    private static String localName(String uri) {
        int i = uri.lastIndexOf('#');
        return (i >= 0 && i + 1 < uri.length()) ? uri.substring(i + 1) : uri;
    }
    private static boolean sameConstraintSet(List<String> c1, List<String> c2) {
        return new HashSet<>(c1).equals(new HashSet<>(c2));
    }

    /* --- DTO -------------------------------------------------------- */
    static class ModeCondRow {
        final String modeUri;
        final String startingCondition;
        final String recordUri;
        ModeCondRow(String mu, String sc, String ru) {
            this.modeUri = mu; this.startingCondition = sc; this.recordUri = ru;
        }
        @Override public String toString() {
            return '{' + localName(modeUri) + ", SC=\"" + startingCondition + "\"}";
        }
    }
}
