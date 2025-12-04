package org.example.drumboiler.reasoner;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.example.drumboiler.rdf.DrumBoilerVocabulary;
import org.example.Prove_helper;

import java.util.*;

/**
 * ModeChange builtin for the DrumBoiler states (uses Ts, Ps).
 * Logic mirrors the Oven example: compare state constraints, then check start/end conditions via KeYmaeraX.
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
        if (args[0].isURI() && args[1].isURI()) {
            String s1 = args[0].getURI();
            String s2 = args[1].getURI();
            if (s1.equals(s2)) return false;

            List<String> c1 = gatherConstraints(s1);
            List<String> c2 = gatherConstraints(s2);
            if (!sameConstraints(c1, c2)) return false;

            List<ModeCond> m1List = gatherModeConds(s1);
            List<ModeCond> m2List = gatherModeConds(s2);

            for (ModeCond m1 : m1List) {
                String f1 = buildImplication(c1, m1.start);
                String r1 = Prove_helper.prove(f1);
                if (!"false".equals(r1)) continue; // implication succeeds if prover returns false
                if (m1.end == null || m1.end.isBlank()) continue;
                for (ModeCond m2 : m2List) {
                    if (m1.uri.equals(m2.uri)) continue;
                    if (m2.start == null || m2.start.isBlank()) continue;
                    String f2 = buildEquiv(m1.end, m2.start);
                    String r2 = Prove_helper.prove(f2);
                    if ("true".equals(r2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> gatherConstraints(String stateUri) {
        String sparql = """
                PREFIX sh: <http://www.w3.org/ns/shacl#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX pre: <http://anonymous.example.org#>
                SELECT DISTINCT ?var ?min ?max WHERE {
                  BIND(<%s> AS ?s)
                  ?s rdf:type pre:State ;
                     pre:hasShape ?shape .
                  ?shape sh:property ?ps .
                  ?ps sh:path ?var .
                  OPTIONAL { ?ps sh:minInclusive ?min }
                  OPTIONAL { ?ps sh:maxInclusive ?max }
                  FILTER (?var IN (pre:Ts, pre:Ps))
                }
                """.formatted(stateUri);
        List<String> out = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String var = sol.getResource("var").getLocalName().replace("_", "");
                if (sol.contains("min")) out.add(var + ">=" + sol.get("min").asLiteral().getLexicalForm());
                if (sol.contains("max")) out.add(var + "<=" + sol.get("max").asLiteral().getLexicalForm());
            }
        }
        return out;
    }

    private List<ModeCond> gatherModeConds(String stateUri) {
        String sparql = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX pre: <http://anonymous.example.org#>
                SELECT DISTINCT ?mode ?start ?end WHERE {
                  BIND(<%s> AS ?s)
                  ?s rdf:type pre:State ;
                     pre:hasMode ?mode .
                  OPTIONAL { ?mode pre:startCondition ?start }
                  OPTIONAL { ?mode pre:endCondition ?end }
                }
                """.formatted(stateUri);
        List<ModeCond> out = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String uri = sol.getResource("mode").getURI();
                String start = sol.contains("start") ? sol.get("start").asLiteral().getLexicalForm() : "";
                String end = sol.contains("end") ? sol.get("end").asLiteral().getLexicalForm() : "";
                out.add(new ModeCond(uri, start, end));
            }
        }
        return out;
    }

    private String buildImplication(List<String> ante, String cons) {
        String a = String.join(" & ", ante);
        return """
                ArchiveEntry "%s"
                ProgramVariables
                  Real Ts, Ps;
                End.
                Problem
                  (%s) -> (%s)
                End.
                End.
                """.formatted(UUID.randomUUID(), a, cons);
    }

    private String buildEquiv(String left, String right) {
        return """
                ArchiveEntry "%s"
                ProgramVariables
                  Real Ts, Ps;
                End.
                Problem
                  (%s) <-> (%s)
                End.
                End.
                """.formatted(UUID.randomUUID(), left, right);
    }

    private boolean sameConstraints(List<String> c1, List<String> c2) {
        return new HashSet<>(c1).equals(new HashSet<>(c2));
    }

    record ModeCond(String uri, String start, String end) {}
}
