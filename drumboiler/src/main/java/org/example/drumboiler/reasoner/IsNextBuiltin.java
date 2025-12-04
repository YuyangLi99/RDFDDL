package org.example.drumboiler.reasoner;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.example.Prove_helper;

import java.util.*;

/**
 * isNext builtin for drumboiler (Ts, Ps). Mirrors Oven's pattern: compare constraints, optionally modes, prove with KeYmaeraX.
 * ODE is not read from RDF (not materialized), so we only use state constraints and domains.
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
            String s1 = args[0].getURI();
            String s2 = args[1].getURI();
            if (s1.equals(s2)) return false;

            List<String> c1 = gatherConstraints(s1);
            List<String> c2 = gatherConstraints(s2);
            if (c1.isEmpty() || c2.isEmpty()) return false;

            String formula = buildFormula(c1, c2);
            String result = Prove_helper.prove(formula);
            return "false".equals(result); // refutable -> reachable
        }
        return false;
    }

    private List<String> gatherConstraints(String stateUri) {
        String sparql = """
                PREFIX sh: <http://www.w3.org/ns/shacl#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX pre: <http://anonymous.example.org#>
                SELECT DISTINCT ?var ?min ?max ?val WHERE {
                  BIND(<%s> AS ?s)
                  ?s rdf:type pre:State ;
                     pre:hasShape ?shape .
                  ?shape sh:property ?ps .
                  ?ps sh:path ?var .
                  FILTER (?var IN (pre:Ts, pre:Ps))
                  OPTIONAL { ?ps sh:minInclusive ?min }
                  OPTIONAL { ?ps sh:maxInclusive ?max }
                  OPTIONAL { ?ps sh:hasValue ?val }
                }
                """.formatted(stateUri);
        List<String> out = new ArrayList<>();
        Model m = dataset.getDefaultModel();
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, m)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                String var = sol.getResource("var").getLocalName();
                var = var.replace("_", "");
                if (sol.contains("val")) {
                    out.add(var + "=" + sol.get("val").asLiteral().getLexicalForm());
                }
                if (sol.contains("min")) {
                    out.add(var + ">=" + sol.get("min").asLiteral().getLexicalForm());
                }
                if (sol.contains("max")) {
                    out.add(var + "<=" + sol.get("max").asLiteral().getLexicalForm());
                }
            }
        }
        return out;
    }

    private String buildFormula(List<String> c1, List<String> c2) {
        String phi1 = String.join(" & ", c1);
        String phi2 = String.join(" & ", c2);
        return """
                ArchiveEntry "%s"
                ProgramVariables
                  Real Ts, Ps;
                End.
                Problem
                  (%s)
                  ->
                  (%s)
                End.
                End.
                """.formatted(UUID.randomUUID(), phi1, phi2);
    }
}
