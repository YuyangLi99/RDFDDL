package org.example;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.*;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.vocabulary.RDF;

import java.io.InputStream;
import java.util.*;

public class Validation {

    public static void main(String[] args) {
        System.out.println("Starting SHACL Validation...");

        // Load shapes model
        Model shapesModel = ModelFactory.createDefaultModel();
        try (InputStream in = Validation.class.getResourceAsStream("/knowledgeGraphWithSHACL_original1.ttl")) {
            if (in == null) {
                throw new RuntimeException("Cannot find knowledgeGraphWithSHACL_original1.ttl in resources!");
            }
            // Explicitly parse as Turtle
            RDFDataMgr.read(shapesModel, in, "urn:base:", org.apache.jena.riot.Lang.TURTLE);
            System.out.println("Successfully loaded shapes model!");
        } catch (Exception e) {
            System.err.println("Error loading shapes model: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test multiple scenarios
        System.out.println("\n====== Testing Multiple Device Scenarios ======\n");

        // Scenario 1: Valid - Heater=On, x=45; Homogenizer=Off, p=9 (s112Shape)
        testScenario(shapesModel, "OnMode", 45, "OffMode", 9,
                "Scenario 1: Heater=On, x=45; Homogenizer=Off, p=9 (should match s112Shape)");

        // Scenario 2: Valid - Heater=Off, x=50; Homogenizer=Off, p=8 (s122Shape)
        testScenario(shapesModel, "OffMode", 50, "OffMode", 8,
                "Scenario 2: Heater=Off, x=50; Homogenizer=Off, p=8 (should match s122Shape)");

        // Scenario 3: Valid - Heater=On, x=60; Homogenizer=On, p=15 (s311Shape)
        testScenario(shapesModel, "OnMode", 60, "OnMode", 15,
                "Scenario 3: Heater=On, x=60; Homogenizer=On, p=15 (should match s311Shape)");

        // Scenario 4: Invalid - Heater=On, x=110; Homogenizer=Off, p=2 (match NO shapes)
        testScenario(shapesModel, "OnMode", 110, "OffMode", 2,
                "Scenario 4: Heater=On, x=110; Homogenizer=Off, p=2 (Invalid - out of range)");

        // Scenario 5: Invalid - Heater=Off, x=30; Homogenizer=On, p=50 (match NO shapes)
        testScenario(shapesModel, "OffMode", 30, "OnMode", 50,
                "Scenario 5: Heater=Off, x=30; Homogenizer=On, p=50 (Invalid - conflicting requirements)");

    }

    private static void testScenario(Model shapesModel, String heaterMode, int xValue,
                                     String homogenizerMode, int pValue, String scenarioName) {
        System.out.println("\n--- " + scenarioName + " ---");

        Model dataModel = createDataModel(heaterMode, xValue, homogenizerMode, pValue);

        // Execute SHACL validation
        ValidationReport report = ShaclValidator.get().validate(
                shapesModel.getGraph(),
                dataModel.getGraph(),
                null
        );

        // Only consider main shapes (those with sh:targetClass pre:State)
        Set<Resource> mainNodeShapes = gatherMainNodeShapes(shapesModel);
        Map<Resource, Resource> propertyShapeToNodeShape = mapPropertyShapeToParent(shapesModel, mainNodeShapes);

        Set<Resource> violatedMainShapes = new HashSet<>();
        for (ReportEntry entry : report.getEntries()) {
            org.apache.jena.graph.Node src = entry.source();
            if (src == null) continue;

            Resource srcRes = shapesModel.wrapAsResource(src);
            if (mainNodeShapes.contains(srcRes)) {
                violatedMainShapes.add(srcRes);
            } else {
                // Check if this violation comes from a property shape of a main shape
                Resource parentMainShape = propertyShapeToNodeShape.get(srcRes);
                if (parentMainShape != null) {
                    violatedMainShapes.add(parentMainShape);
                }
            }
        }

        Set<Resource> satisfiedMainShapes = new HashSet<>(mainNodeShapes);
        satisfiedMainShapes.removeAll(violatedMainShapes);

        // Print results
        System.out.println("Data: Heater=" + heaterMode + ", x=" + xValue +
                "; Homogenizer=" + homogenizerMode + ", p=" + pValue);
        System.out.println("Total main shapes defined: " + mainNodeShapes.size());

        if (satisfiedMainShapes.isEmpty()) {
            System.out.println("✗ No main shapes are satisfied (data is INVALID - does not conform to any expected state)");
        } else {
            System.out.println("✓ Satisfied main shapes (" + satisfiedMainShapes.size() + "):");
            for (Resource shape : satisfiedMainShapes) {
                String shapeName = getShapeName(shape);
                System.out.println("  - " + shapeName);
            }
        }

        if (!violatedMainShapes.isEmpty()) {
            System.out.println("✗ Violated main shapes (" + violatedMainShapes.size() + "):");
            for (Resource shape : violatedMainShapes) {
                String shapeName = getShapeName(shape);
                System.out.println("  - " + shapeName);
            }
        }

        // Print detailed violations for invalid cases
        if (satisfiedMainShapes.isEmpty() && !report.getEntries().isEmpty()) {
            System.out.println("Violation details (first 3):");
            int count = 0;
            for (ReportEntry entry : report.getEntries()) {
                if (count >= 3) break;
                System.out.println("  • " + entry.message());
                System.out.println("    Focus: " + entry.focusNode());
                System.out.println("    Source: " + entry.source());
                count++;
            }
            if (report.getEntries().size() > 3) {
                System.out.println("  ... and " + (report.getEntries().size() - 3) + " more violations");
            }
        }

        // The data is valid if at least one main shape is satisfied
        boolean isValid = !satisfiedMainShapes.isEmpty();
        System.out.println("Overall conformance: " + (isValid ? "✓ VALID" : "✗ INVALID"));
    }

    /**
     * Creates a data model with specified Heater and Homogenizer configurations
     */
    private static Model createDataModel(String heaterMode, int xValue,
                                         String homogenizerMode, int pValue) {
        Model data = ModelFactory.createDefaultModel();
        String pre = "https://anonymous.example.org#";

        // Heater with specified mode and x value
        Resource heater = data.createResource(pre + "Heater");
        heater.addProperty(RDF.type, data.createResource(pre + "Device"));
        heater.addProperty(data.createProperty(pre, "mode"), data.createResource(pre + heaterMode));
        heater.addLiteral(data.createProperty(pre, "x"), xValue);

        // Homogenizer with specified mode and p value
        Resource homogenizer = data.createResource(pre + "Homogenizer");
        homogenizer.addProperty(RDF.type, data.createResource(pre + "Device"));
        homogenizer.addProperty(data.createProperty(pre, "mode"), data.createResource(pre + homogenizerMode));
        homogenizer.addLiteral(data.createProperty(pre, "P"), pValue);

        // realTimeState: references Heater, Homogenizer
        // typed as ex:State so that shapes with sh:targetClass ex:State will pick it up
        Resource realTimeState = data.createResource(pre + "realTimeState");
        realTimeState.addProperty(RDF.type, data.createResource(pre + "State"));
        realTimeState.addProperty(data.createProperty(pre, "hasHeater"), heater);
        realTimeState.addProperty(data.createProperty(pre, "hasHomogenizer"), homogenizer);

        return data;
    }

    /**
     * Extract a readable shape name from URI
     */
    private static String getShapeName(Resource shape) {
        String uri = shape.getURI();
        if (uri != null && uri.contains("#")) {
            return uri.substring(uri.lastIndexOf("#") + 1);
        }
        return uri != null ? uri : shape.toString();
    }

    /**
     * Gather only main NodeShapes that have sh:targetClass pre:State
     * This excludes sub-shapes like HeaterShape, HomogenizerShape, etc.
     */
    private static Set<Resource> gatherMainNodeShapes(Model shapesModel) {
        Set<Resource> results = new HashSet<>();
        Resource nodeShapeClass = shapesModel.createResource("http://www.w3.org/ns/shacl#NodeShape");
        Property targetClassProperty = shapesModel.createProperty("http://www.w3.org/ns/shacl#targetClass");
        Resource stateClass = shapesModel.createResource("https://anonymous.example.org#State");

        ResIterator it = shapesModel.listResourcesWithProperty(RDF.type, nodeShapeClass);
        while (it.hasNext()) {
            Resource shape = it.nextResource();
            // Only include shapes that target the State class
            if (shape.hasProperty(targetClassProperty, stateClass)) {
                results.add(shape);
            }
        }
        it.close();
        return results;
    }

    private static Set<Resource> gatherAllNodeShapes(Model shapesModel) {
        Set<Resource> results = new HashSet<>();
        Resource nodeShapeClass = shapesModel.createResource("http://www.w3.org/ns/shacl#NodeShape");
        ResIterator it = shapesModel.listResourcesWithProperty(RDF.type, nodeShapeClass);
        while (it.hasNext()) {
            results.add(it.nextResource());
        }
        it.close();
        return results;
    }

    private static Map<Resource, Resource> mapPropertyShapeToParent(Model shapesModel, Set<Resource> nodeShapes) {
        Map<Resource, Resource> result = new HashMap<>();
        Property prop = shapesModel.createProperty("http://www.w3.org/ns/shacl#property");

        for (Resource ns : nodeShapes) {
            StmtIterator it = shapesModel.listStatements(ns, prop, (RDFNode) null);
            while (it.hasNext()) {
                Statement st = it.next();
                RDFNode obj = st.getObject();
                if (obj.isResource()) {
                    result.put(obj.asResource(), ns);
                }
            }
            it.close();
        }
        return result;
    }
}