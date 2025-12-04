package org.example.drumboiler.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.example.drumboiler.fmu.FmuVariable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Placeholder implementation that fabricates reasonable defaults.
 * Replace with a real LLM client by implementing {@link LlmAssistant}.
 */
public class DummyLlmAssistant implements LlmAssistant {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Property SH_PROPERTY = ResourceFactory.createProperty(SHACL.property.getURI());
    private static final Property SH_PATH = ResourceFactory.createProperty(SHACL.path.getURI());
    private static final Property SH_HAS_VALUE = ResourceFactory.createProperty(SHACL.hasValue.getURI());
    private static final Property SH_MIN_INCL = ResourceFactory.createProperty(SHACL.minInclusive.getURI());
    private static final Property SH_MAX_INCL = ResourceFactory.createProperty(SHACL.maxInclusive.getURI());
    private static final Property SH_MIN_EXCL = ResourceFactory.createProperty(SHACL.minExclusive.getURI());
    private static final Property SH_MAX_EXCL = ResourceFactory.createProperty(SHACL.maxExclusive.getURI());

    @Override
    public String suggestConfig(List<FmuVariable> variables, String processDescription) {
        Map<String, String> preferred = Map.of(
                "T_S", "Ts",
                "p_S", "Ps",
                "V_l", "Vl",
                "qm_S", "qmS"
        );
        Map<String, FmuVariable> chosen = pickPreferredVariables(variables, preferred);
        List<String> trackedNames = chosen.values().stream()
                .map(v -> preferred.getOrDefault(v.getName(), simplify(v.getName())))
                .toList();

        ObjectNode root = mapper.createObjectNode();
        ArrayNode tracked = mapper.createArrayNode();
        trackedNames.forEach(tracked::add);
        root.set("trackedVariables", tracked);

        ArrayNode states = mapper.createArrayNode();
        states.add(stateLow(chosen));
        states.add(stateNormal(chosen));
        states.add(stateHigh(chosen));
        root.set("stateShapes", states);

        ArrayNode modes = mapper.createArrayNode();
        modes.add(modeFlowing());
        modes.add(modeIdle());
        root.set("modes", modes);

        ArrayNode transitions = mapper.createArrayNode();
        transitions.add(transition("sLowPressure", "sNormal"));
        transitions.add(transition("sNormal", "sHighPressure"));
        root.set("transitions", transitions);

        ObjectNode ode = mapper.createObjectNode();
        ode.put("evolutionDomain", "pS >= 0 & Vl >= 0");
        ArrayNode equations = mapper.createArrayNode();
        equations.add(eq("TS", "-0.05*(TS-320) + 0.01*(pS-90)"));
        equations.add(eq("pS", "0.02*(TS-300) - 0.03*(pS-95)"));
        equations.add(eq("Vl", "-0.001*qmS"));
        equations.add(eq("qmS", "0"));
        ode.set("equations", equations);
        root.set("ode", ode);

        return root.toPrettyString();
    }

    @Override
    public String summarizeShapes(Model model) {
        StringBuilder sb = new StringBuilder();
        List<Resource> shapes = model.listResourcesWithProperty(RDF.type, SHACL.NodeShape)
                .toList()
                .stream()
                .sorted(Comparator.comparing(Resource::getURI, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
        for (Resource shape : shapes) {
            String name = shape.getLocalName() != null ? shape.getLocalName() : shape.getURI();
            sb.append("### State ").append(name.replace("Shape", "")).append("\n");
            StmtIterator labelIter = shape.listProperties(model.createProperty("http://www.w3.org/2000/01/rdf-schema#label"));
            if (labelIter.hasNext()) {
                sb.append(labelIter.nextStatement().getObject().asLiteral().getString()).append("\n\n");
            }
            StmtIterator props = shape.listProperties(SH_PROPERTY);
            while (props.hasNext()) {
                RDFNode node = props.nextStatement().getObject();
                if (!node.isResource()) continue;
                Resource ps = node.asResource();
                Statement pathStmt = ps.getProperty(SH_PATH);
                String pathStr = "unknown";
                if (pathStmt != null) {
                    RDFNode pObj = pathStmt.getObject();
                    if (pObj.isResource() && pObj.asResource().getLocalName() != null) {
                        pathStr = pObj.asResource().getLocalName();
                    } else if (pObj.isURIResource()) {
                        pathStr = pObj.asResource().getURI();
                    }
                }
                List<String> bounds = new ArrayList<>();
                addIfLiteral(bounds, ps, SH_HAS_VALUE, "= ");
                addIfLiteral(bounds, ps, SH_MIN_INCL, ">= ");
                addIfLiteral(bounds, ps, SH_MAX_INCL, "<= ");
                addIfLiteral(bounds, ps, SH_MIN_EXCL, "> ");
                addIfLiteral(bounds, ps, SH_MAX_EXCL, "< ");
                sb.append("- ").append(pathStr).append(": ").append(String.join(", ", bounds)).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private ObjectNode stateLow(Map<String, FmuVariable> vars) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", "sLowPressureShape");
        node.put("state", "sLowPressure");
        node.put("label", "Low pressure region");
        node.put("mode", "Flowing");
        ArrayNode constraints = mapper.createArrayNode();
        addMaxConstraint(constraints, vars.get("Ps"), "Ps", 70.0);
        addMaxConstraint(constraints, vars.get("Ts"), "Ts", 260.0);
        node.set("constraints", constraints);
        return node;
    }

    private ObjectNode stateNormal(Map<String, FmuVariable> vars) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", "sNormalShape");
        node.put("state", "sNormal");
        node.put("label", "Nominal operation");
        node.put("mode", "Flowing");
        ArrayNode constraints = mapper.createArrayNode();
        addRangeConstraint(constraints, vars.get("Ts"), "Ts", 250.0, 320.0);
        addRangeConstraint(constraints, vars.get("Ps"), "Ps", 70.0, 95.0);
        addRangeConstraint(constraints, vars.get("Vl"), "Vl", 5.0, 15.0);
        node.set("constraints", constraints);
        return node;
    }

    private ObjectNode stateHigh(Map<String, FmuVariable> vars) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", "sHighPressureShape");
        node.put("state", "sHighPressure");
        node.put("label", "High pressure region");
        node.put("mode", "Flowing");
        ArrayNode constraints = mapper.createArrayNode();
        addMinConstraint(constraints, vars.get("Ps"), "Ps", 95.0);
        addMinConstraint(constraints, vars.get("Ts"), "Ts", 320.0);
        node.set("constraints", constraints);
        return node;
    }

    private ObjectNode transition(String from, String to) {
        ObjectNode node = mapper.createObjectNode();
        node.put("from", from);
        node.put("to", to);
        return node;
    }

    private ObjectNode modeFlowing() {
        ObjectNode mode = mapper.createObjectNode();
        mode.put("id", "Flowing");
        mode.put("label", "Pump/Valve active (linearized)");
        mode.put("evolutionDomain", "Ps >= 0 & Vl >= 0");
        ArrayNode eqs = mapper.createArrayNode();
        eqs.add(eq("Ts", "-0.05*(Ts-320) + 0.01*(Ps-90)"));
        eqs.add(eq("Ps", "0.02*(Ts-300) - 0.03*(Ps-95)"));
        eqs.add(eq("Vl", "-0.001*qmS"));
        eqs.add(eq("qmS", "0"));
        mode.set("equations", eqs);
        return mode;
    }

    private ObjectNode modeIdle() {
        ObjectNode mode = mapper.createObjectNode();
        mode.put("id", "Idle");
        mode.put("label", "Pump/Valve idle (cooldown)");
        mode.put("evolutionDomain", "Ps >= 0 & Vl >= 0");
        ArrayNode eqs = mapper.createArrayNode();
        eqs.add(eq("Ts", "-0.02*(Ts-25)"));
        eqs.add(eq("Ps", "-0.05*(Ps)"));
        eqs.add(eq("Vl", "0"));
        eqs.add(eq("qmS", "0"));
        mode.set("equations", eqs);
        return mode;
    }

    private ObjectNode eq(String var, String expr) {
        ObjectNode node = mapper.createObjectNode();
        node.put("variable", var);
        node.put("expression", expr);
        return node;
    }

    private String simplify(String raw) {
        return raw.replaceAll("[^A-Za-z0-9]", "");
    }

    private void addIfLiteral(List<String> bounds, Resource ps, Property prop, String prefix) {
        Property p = ResourceFactory.createProperty(prop.getURI());
        Statement st = ps.getProperty(p);
        if (st != null && st.getObject().isLiteral()) {
            bounds.add(prefix + st.getObject().asLiteral().getLexicalForm());
        }
    }

    private Map<String, FmuVariable> pickPreferredVariables(List<FmuVariable> variables, Map<String, String> preferred) {
        Map<String, FmuVariable> picked = new HashMap<>();
        // first pass: exact name matches
        for (FmuVariable v : variables) {
            if (preferred.containsKey(v.getName())) {
                picked.put(preferred.get(v.getName()), v);
            }
        }
        // second pass: contain substring
        for (FmuVariable v : variables) {
            for (String key : preferred.keySet()) {
                if (!picked.containsKey(preferred.get(key)) && v.getName().contains(key)) {
                    picked.put(preferred.get(key), v);
                }
            }
        }
        // fallback: fill remaining slots with first Reals
        for (FmuVariable v : variables) {
            if (picked.size() >= preferred.size()) break;
            String simp = simplify(v.getName());
            if (!picked.containsKey(simp)) {
                picked.putIfAbsent(simp, v);
            }
        }
        return picked;
    }

    private void addRangeConstraint(ArrayNode constraints, FmuVariable v, String name, double min, double max) {
        ObjectNode c = mapper.createObjectNode();
        c.put("variable", name);
        c.put("minInclusive", min);
        c.put("maxInclusive", max);
        constraints.add(c);
    }

    private void addMaxConstraint(ArrayNode constraints, FmuVariable v, String name, double max) {
        ObjectNode c = mapper.createObjectNode();
        c.put("variable", name);
        c.put("maxInclusive", max);
        constraints.add(c);
    }

    private void addMinConstraint(ArrayNode constraints, FmuVariable v, String name, double min) {
        ObjectNode c = mapper.createObjectNode();
        c.put("variable", name);
        c.put("minInclusive", min);
        constraints.add(c);
    }
}
