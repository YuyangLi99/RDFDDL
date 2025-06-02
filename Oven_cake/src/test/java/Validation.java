package org.example;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.*;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.vocabulary.RDF;

import java.io.FileInputStream;
import java.io.File;
import java.util.*;

public class Validation {

    public static void main(String[] args) {
        System.out.println("Starting SHACL Validation...");

        // Load shapes model
        Model shapesModel = ModelFactory.createDefaultModel();

        String relativePath = "src/test/resources/knowledgeGraphWithSHACL_oven_safe.ttl";

        try {
            File file = new File(relativePath);
            System.out.println("Looking for file at: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());

            if (!file.exists()) {
                throw new RuntimeException("File not found at: " + relativePath);
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                RDFDataMgr.read(shapesModel, fis, "urn:base:", org.apache.jena.riot.Lang.TURTLE);
                System.out.println("Successfully loaded shapes model!");
            }

        } catch (Exception e) {
            System.err.println("Error loading shapes model: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test multiple scenarios
        System.out.println("\n====== Testing Multiple Oven Scenarios ======\n");

        // Scenario 1: Oven=On, x=60 (should ONLY match s12Shape)
        testScenario(shapesModel, "OnMode", 60, "Scenario 1: Oven=On, x=60");

        // Scenario 2: Oven=Off, x=150 (should ONLY match s11Shape)
        testScenario(shapesModel, "OffMode", 150, "Scenario 2: Oven=Off, x=150");

        // Scenario 3: Oven=On, x=190 (should ONLY match s22Shape)
        testScenario(shapesModel, "OnMode", 190, "Scenario 3: Oven=On, x=190");

        // Scenario 4: Oven=Off, x=200 (should ONLY match s21Shape)
        testScenario(shapesModel, "OffMode", 200, "Scenario 4: Oven=Off, x=200");

        // Scenario 5: Invalid - Oven=On, x=250 (should match NO shapes)
        testScenario(shapesModel, "OnMode", 250, "Scenario 5: Oven=On, x=250 (Invalid)");

    }

    private static void testScenario(Model shapesModel, String mode, int temperature, String scenarioName) {
        System.out.println("\n--- " + scenarioName + " ---");

        Model dataModel = createDataModel(mode, temperature);

        // Execute SHACL validation
        ValidationReport report = ShaclValidator.get().validate(
                shapesModel.getGraph(),
                dataModel.getGraph(),
                null
        );

        // Determine which main shapes (not OvenShapes) are satisfied
        Set<Resource> mainShapes = gatherMainShapes(shapesModel);
        Set<Resource> violatedMainShapes = new HashSet<>();

        // Check violations for main shapes only
        for (ReportEntry entry : report.getEntries()) {
            org.apache.jena.graph.Node src = entry.source();
            if (src == null) continue;

            Resource srcRes = shapesModel.wrapAsResource(src);

            // Find the main shape that contains this property shape
            for (Resource mainShape : mainShapes) {
                if (isPropertyShapeOf(shapesModel, srcRes, mainShape)) {
                    violatedMainShapes.add(mainShape);
                    break;
                }
            }
        }

        // Find satisfied main shapes
        Set<Resource> satisfiedMainShapes = new HashSet<>(mainShapes);
        satisfiedMainShapes.removeAll(violatedMainShapes);

        // Print results
        System.out.println("Data: " + mode + ", x=" + temperature);
        System.out.println("Expected shape: " + determineExpectedShape(mode, temperature));

        if (satisfiedMainShapes.isEmpty()) {
            System.out.println("✗ No shapes are satisfied (data is invalid)");
        } else {
            System.out.println("✓ Satisfied shapes:");
            for (Resource shape : satisfiedMainShapes) {
                String shapeName = shape.getURI().substring(shape.getURI().lastIndexOf("#") + 1);
                System.out.println("  - " + shapeName);
            }
        }

        if (!violatedMainShapes.isEmpty()) {
            System.out.println("✗ Violated shapes:");
            for (Resource shape : violatedMainShapes) {
                String shapeName = shape.getURI().substring(shape.getURI().lastIndexOf("#") + 1);
                System.out.println("  - " + shapeName);
            }
        }
    }

    /**
     * Creates a data model with an Oven in specified mode and temperature
     */
    private static Model createDataModel(String mode, int temperature) {
        Model data = ModelFactory.createDefaultModel();
        String pre = "http://anonymous.example.org#";

        // Create Oven with specified mode and temperature
        Resource oven = data.createResource(pre + "Oven");
        oven.addProperty(RDF.type, data.createResource(pre + "Device"));
        oven.addProperty(data.createProperty(pre, "mode"), data.createResource(pre + mode));
        oven.addLiteral(data.createProperty(pre, "x"), temperature);

        // Create realTimeState that references the Oven
        Resource realTimeState = data.createResource(pre + "realTimeState");
        realTimeState.addProperty(RDF.type, data.createResource(pre + "State"));
        realTimeState.addProperty(data.createProperty(pre, "hasOven"), oven);

        return data;
    }

    private static String determineExpectedShape(String mode, int temperature) {
        if (mode.equals("OffMode")) {
            if (temperature <= 180) {
                return "s11Shape (Off, x≤180)";
            } else {
                return "s21Shape (Off, x≥180)";
            }
        } else { // OnMode
            if (temperature <= 180) {
                return "s12Shape (On, x≤180)";
            } else {
                return "s22Shape (On, x≥180)";
            }
        }
    }

    /**
     * Gather only main shapes (s11Shape, s12Shape, s21Shape, s22Shape)
     * excluding OvenShapes
     */
    private static Set<Resource> gatherMainShapes(Model shapesModel) {
        Set<Resource> results = new HashSet<>();
        Resource nodeShapeClass = shapesModel.createResource("http://www.w3.org/ns/shacl#NodeShape");
        Property targetClass = shapesModel.createProperty("http://www.w3.org/ns/shacl#targetClass");

        ResIterator it = shapesModel.listResourcesWithProperty(RDF.type, nodeShapeClass);
        while (it.hasNext()) {
            Resource shape = it.nextResource();
            // Only collect shapes with targetClass (main shapes)
            if (shape.hasProperty(targetClass)) {
                results.add(shape);
            }
        }
        it.close();
        return results;
    }

    /**
     * Check if a shape is a property shape of a main shape
     */
    private static boolean isPropertyShapeOf(Model model, Resource propertyShape, Resource mainShape) {
        Property prop = model.createProperty("http://www.w3.org/ns/shacl#property");
        Property node = model.createProperty("http://www.w3.org/ns/shacl#node");

        StmtIterator it = model.listStatements(mainShape, prop, (RDFNode) null);
        while (it.hasNext()) {
            Statement st = it.next();
            if (st.getObject().isResource()) {
                Resource propShape = st.getObject().asResource();

                // Check if this property shape references our property shape via sh:node
                Statement nodeStmt = propShape.getProperty(node);
                if (nodeStmt != null && nodeStmt.getObject().isResource()) {
                    if (nodeStmt.getObject().asResource().equals(propertyShape)) {
                        return true;
                    }
                }

                // Also check if this IS the property shape
                if (propShape.equals(propertyShape)) {
                    return true;
                }
            }
        }
        it.close();
        return false;
    }
}