package org.example.drumboiler.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Minimal vocabulary helper for the anonymous drum boiler namespace.
 */
public final class DrumBoilerVocabulary {
    public static final String NS = "http://anonymous.example.org#";

    public static final Resource VARIABLE = ResourceFactory.createResource(NS + "Variable");
    public static final Resource STATE = ResourceFactory.createResource(NS + "State");
    public static final Resource MODE = ResourceFactory.createResource(NS + "Mode");

    public static final Property UNIT = ResourceFactory.createProperty(NS, "unit");
    public static final Property HAS_SHAPE = ResourceFactory.createProperty(NS, "hasShape");
    public static final Property HAS_DEVICE = ResourceFactory.createProperty(NS, "hasDevice");
    public static final Property NEXT = ResourceFactory.createProperty(NS, "next");
    public static final Property HAS_MODE = ResourceFactory.createProperty(NS, "hasMode");
    public static final Property START_CONDITION = ResourceFactory.createProperty(NS, "startCondition");
    public static final Property END_CONDITION = ResourceFactory.createProperty(NS, "endCondition");

    private DrumBoilerVocabulary() {
    }
}
