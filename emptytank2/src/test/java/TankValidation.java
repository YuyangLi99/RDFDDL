package org.example;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.*;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.vocabulary.RDF;

import java.io.FileInputStream;
import java.io.File;
import java.util.*;

public class TankValidation {

    public static void main(String[] args) {
        System.out.println("Starting Tank System SHACL Validation...");

        // Load shapes model
        Model shapesModel = ModelFactory.createDefaultModel();

        String relativePath = "src/test/resources/knowledgeGraphWithSHACL_Tank1.ttl";

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
        System.out.println("\n====== Testing Multiple Tank System Scenarios ======\n");

        // Scenario 1: Both tanks off, h1=1.0, h2=0.1 (should match s0Shape)
        testScenario(shapesModel, "OffMode", "OffMode", 1.0, 0.1,
                "Scenario 1: Both OFF, h1=1.0m, h2=0.1m");

        // Scenario 2: Both tanks on, h1=1.0, h2=0.1 (should match s1Shape)
        testScenario(shapesModel, "OnMode", "OnMode", 1.0, 0.1,
                "Scenario 2: Both ON, h1=1.0m, h2=0.1m");

        // Scenario 3: Both tanks off, h1=0.5, h2=0.5 (should match s2Shape)
        testScenario(shapesModel, "OffMode", "OffMode", 0.5, 0.5,
                "Scenario 3: Both OFF, h1=0.5m, h2=0.5m");

        // Scenario 4: Both tanks on, h1=0.0, h2=1.0 (should match s3Shape)
        testScenario(shapesModel, "OnMode", "OnMode", 0.0, 1.0,
                "Scenario 4: Both ON, h1=0.0m, h2=1.0m");

        // Scenario 5: Both tanks off, h1=0.0, h2=1.0 (should match s4Shape)
        testScenario(shapesModel, "OffMode", "OffMode", 0.0, 1.0,
                "Scenario 5: Both OFF, h1=0.0m, h2=1.0m");

        // Scenario 6: Invalid - Mixed modes, should match NO shapes
        testScenario(shapesModel, "OnMode", "OffMode", 0.5, 0.5,
                "Scenario 6: Tank1 ON, Tank2 OFF (Invalid)");

        // Scenario 7: Invalid - Water levels out of range
        testScenario(shapesModel, "OnMode", "OnMode", 1.5, 0.5,
                "Scenario 7: h1=1.5m (out of range)");

        // Scenario 8: Edge case - h1=0.0, h2=0.1, both off
        testScenario(shapesModel, "OffMode", "OffMode", 0.0, 0.1,
                "Scenario 8: Edge case - h1=0.0m, h2=0.1m, both OFF");

        // Scenario 9: Edge case - h1=0.7, h2=0.99, both off (should match s2Shape)
        testScenario(shapesModel, "OffMode", "OffMode", 0.7, 0.99,
                "Scenario 9: Both OFF, h1=0.7m, h2=0.99m");
    }

    private static void testScenario(Model shapesModel, String mode1, String mode2,
                                     double h1, double h2, String scenarioName) {
        System.out.println("\n--- " + scenarioName + " ---");

        Model dataModel = createDataModel(mode1, mode2, h1, h2);

        // Execute SHACL validation
        ValidationReport report = ShaclValidator.get().validate(
                shapesModel.getGraph(),
                dataModel.getGraph(),
                null
        );

        // Determine which main shapes (not Tank1Shape/Tank2Shape) are satisfied
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
        System.out.println("Data: Tank1=" + mode1 + ", h1=" + h1 + "m; Tank2=" + mode2 + ", h2=" + h2 + "m");
        System.out.println("Expected shape: " + determineExpectedShape(mode1, mode2, h1, h2));

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
     * Creates a data model with two Tanks in specified modes and water levels
     */
    private static Model createDataModel(String mode1, String mode2, double h1, double h2) {
        Model data = ModelFactory.createDefaultModel();
        String pre = "http://anonymous.example.org#";
        String schema = "http://schema.org/";

        // Create Tank1 with specified mode and water level
        Resource tank1 = data.createResource(pre + "Tank1");
        tank1.addProperty(RDF.type, data.createResource(schema + "Product"));
        tank1.addProperty(data.createProperty(pre, "mode"), data.createResource(pre + mode1));
        tank1.addLiteral(data.createProperty(pre, "h1"), h1);

        // Create Tank2 with specified mode and water level
        Resource tank2 = data.createResource(pre + "Tank2");
        tank2.addProperty(RDF.type, data.createResource(schema + "Product"));
        tank2.addProperty(data.createProperty(pre, "mode"), data.createResource(pre + mode2));
        tank2.addLiteral(data.createProperty(pre, "h2"), h2);

        // Create realTimeState that references both tanks
        Resource realTimeState = data.createResource(pre + "realTimeState");
        realTimeState.addProperty(RDF.type, data.createResource(pre + "State"));
        realTimeState.addProperty(data.createProperty(pre, "hasTank1"), tank1);
        realTimeState.addProperty(data.createProperty(pre, "hasTank2"), tank2);

        return data;
    }

    private static String determineExpectedShape(String mode1, String mode2, double h1, double h2) {
        // Both tanks must have the same mode in this system
        if (!mode1.equals(mode2)) {
            return "No valid shape (modes must match)";
        }

        if (mode1.equals("OffMode")) {
            if (h1 == 1.0 && h2 == 0.1) {
                return "s0Shape (Both OFF, h1=1.0, h2=0.1)";
            } else if (h1 > 0.0 && h1 < 1.0 && h2 > 0.1 && h2 < 1.0) {
                return "s2Shape (Both OFF, 0<h1<1, 0.1<h2<1)";
            } else if (h1 == 0.0 && h2 == 1.0) {
                return "s4Shape (Both OFF, h1=0, h2=1)";
            }
        } else { // OnMode
            if (h1 == 1.0 && h2 == 0.1) {
                return "s1Shape (Both ON, h1=1.0, h2=0.1)";
            } else if (h1 == 0.0 && h2 == 1.0) {
                return "s3Shape (Both ON, h1=0, h2=1)";
            }
        }

        return "No valid shape";
    }

    /**
     * Gather only main shapes (s0Shape through s4Shape)
     * excluding Tank1Shape/Tank2Shape
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
                    Resource nodeShape = nodeStmt.getObject().asResource();

                    // Check nested property shapes in the node shape
                    StmtIterator nodeIt = model.listStatements(nodeShape, prop, (RDFNode) null);
                    while (nodeIt.hasNext()) {
                        Statement nodeSt = nodeIt.next();
                        if (nodeSt.getObject().isResource() &&
                                nodeSt.getObject().asResource().equals(propertyShape)) {
                            return true;
                        }
                    }
                    nodeIt.close();

                    // Also check if the node shape itself is the property shape
                    if (nodeShape.equals(propertyShape)) {
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