package org.example.drumboiler.rdf;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.example.drumboiler.config.BoilerConfig;
import org.example.drumboiler.config.StateShapeConfig;
import org.example.drumboiler.config.VariableConstraint;
import org.example.drumboiler.fmu.FmuVariable;
import org.example.drumboiler.config.ModeConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Builds the RDF + SHACL artefacts backing the discretised drum boiler states.
 */
public class RdfShaclGenerator {

    private static final Resource SH_NODE_SHAPE = ResourceFactory.createResource(SHACL.NodeShape.getURI());
    private static final Property SH_TARGET_CLASS = ResourceFactory.createProperty(SHACL.targetClass.getURI());
    private static final Property SH_PROPERTY = ResourceFactory.createProperty(SHACL.property.getURI());
    private static final Property SH_PATH = ResourceFactory.createProperty(SHACL.path.getURI());
    private static final Property SH_HAS_VALUE = ResourceFactory.createProperty(SHACL.hasValue.getURI());
    private static final Property SH_MIN_INCLUSIVE = ResourceFactory.createProperty(SHACL.minInclusive.getURI());
    private static final Property SH_MAX_INCLUSIVE = ResourceFactory.createProperty(SHACL.maxInclusive.getURI());
    private static final Property SH_MIN_EXCLUSIVE = ResourceFactory.createProperty(SHACL.minExclusive.getURI());
    private static final Property SH_MAX_EXCLUSIVE = ResourceFactory.createProperty(SHACL.maxExclusive.getURI());

    /**
     * Generates the RDF/SHACL model and serialises it as Turtle.
     *
     * @param variables parsed FMU variables
     * @param config    discretisation config
     * @param output    Turtle file to write
     */
    public void generateRdfShacl(List<FmuVariable> variables,
                                 BoilerConfig config,
                                 Path output) throws IOException {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("pre", DrumBoilerVocabulary.NS);
        model.setNsPrefix("sh", SHACL.NS);
        model.setNsPrefix("rdf", RDF.uri);
        model.setNsPrefix("xsd", XSD.NS);

        Map<String, FmuVariable> variableByName = new LinkedHashMap<>();
        for (FmuVariable variable : variables) {
            variableByName.put(variable.getName(), variable);
        }

        List<String> tracked = config.getTrackedVariables().isEmpty()
                ? new ArrayList<>(variableByName.keySet())
                : config.getTrackedVariables();

        // declare modes
        for (ModeConfig mode : config.getModes()) {
            Resource modeRes = model.createResource(DrumBoilerVocabulary.NS + mode.getId());
            modeRes.addProperty(RDF.type, DrumBoilerVocabulary.MODE);
            if (mode.getLabel() != null) {
                modeRes.addProperty(RDFS.label, mode.getLabel());
            }
            if (mode.getStartCondition() != null) {
                modeRes.addProperty(DrumBoilerVocabulary.START_CONDITION, mode.getStartCondition());
            }
            if (mode.getEndCondition() != null) {
                modeRes.addProperty(DrumBoilerVocabulary.END_CONDITION, mode.getEndCondition());
            }
        }

        for (String varName : tracked) {
            Resource resource = model.createResource(DrumBoilerVocabulary.NS + varName);
            resource.addProperty(RDF.type, DrumBoilerVocabulary.VARIABLE);
            FmuVariable fmuVariable = resolveFmuVariable(varName, variableByName);
            if (fmuVariable != null && fmuVariable.getUnit() != null && !fmuVariable.getUnit().isEmpty()) {
                resource.addProperty(DrumBoilerVocabulary.UNIT, fmuVariable.getUnit());
            }
        }

        for (StateShapeConfig shapeConfig : config.getStateShapes()) {
            Resource shape = model.createResource(DrumBoilerVocabulary.NS + shapeConfig.getId());
            shape.addProperty(RDF.type, SH_NODE_SHAPE);
            shape.addProperty(SH_TARGET_CLASS, DrumBoilerVocabulary.STATE);
            if (shapeConfig.getLabel() != null) {
                shape.addProperty(RDFS.label, shapeConfig.getLabel());
            }

            for (VariableConstraint constraint : shapeConfig.getConstraints()) {
                Resource propertyShape = model.createResource();
                Property path = model.createProperty(DrumBoilerVocabulary.NS, constraint.getVariable());
                propertyShape.addProperty(SH_PATH, path);
                addConstraintLiteral(model, propertyShape, SH_HAS_VALUE, constraint.getHasValue());
                addConstraintLiteral(model, propertyShape, SH_MIN_INCLUSIVE, constraint.getMinInclusive());
                addConstraintLiteral(model, propertyShape, SH_MAX_INCLUSIVE, constraint.getMaxInclusive());
                addConstraintLiteral(model, propertyShape, SH_MIN_EXCLUSIVE, constraint.getMinExclusive());
                addConstraintLiteral(model, propertyShape, SH_MAX_EXCLUSIVE, constraint.getMaxExclusive());
                shape.addProperty(SH_PROPERTY, propertyShape);
            }

            if (shapeConfig.getState() != null) {
                Resource state = model.createResource(DrumBoilerVocabulary.NS + shapeConfig.getState());
                state.addProperty(RDF.type, DrumBoilerVocabulary.STATE);
                state.addProperty(DrumBoilerVocabulary.HAS_SHAPE, shape);
                if (shapeConfig.getMode() != null) {
                    Resource modeRes = model.createResource(DrumBoilerVocabulary.NS + shapeConfig.getMode());
                    state.addProperty(DrumBoilerVocabulary.HAS_MODE, modeRes);
                }
            }
        }

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        try (OutputStream os = Files.newOutputStream(output)) {
            RDFDataMgr.write(os, model, RDFFormat.TURTLE_PRETTY);
        }
    }

    private void addConstraintLiteral(Model model, Resource propertyShape, Property predicate, Double value) {
        if (value == null) {
            return;
        }
        propertyShape.addLiteral(predicate, model.createTypedLiteral(value));
    }

    private FmuVariable resolveFmuVariable(String varName, Map<String, FmuVariable> variableByName) {
        if (variableByName.containsKey(varName)) {
            return variableByName.get(varName);
        }
        // allow simple-name forms like Ts/Ps to match FMU names T_S/p_S
        if ("Ts".equals(varName) && variableByName.containsKey("T_S")) {
            return variableByName.get("T_S");
        }
        if ("Ps".equals(varName) && variableByName.containsKey("p_S")) {
            return variableByName.get("p_S");
        }
        return null;
    }
}
