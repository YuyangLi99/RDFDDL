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

        // 1) Load shapes model (the one containing s112, s122, s211, etc.)
        Model shapesModel = ModelFactory.createDefaultModel();
        try (InputStream in = Validation.class.getResourceAsStream("/knowledgeGraphWithSHACL_original1.ttl")) {
            if (in == null) {
                throw new RuntimeException("Cannot find knowledgeGraphWithSHACL_original1.ttl in resources!");
            }
            // Explicitly parse as Turtle
            RDFDataMgr.read(shapesModel, in, "urn:base:", org.apache.jena.riot.Lang.TURTLE);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 2) Create real-time data (Heater=On, x=45; Homogenizer=Off, p=9)
        Model dataModel = createDataModel();

        // 3) Execute SHACL validation
        ValidationReport report = ShaclValidator.get().validate(
                shapesModel.getGraph(),
                dataModel.getGraph(),
                null
        );

        // 4) Print basic result
        System.out.println("\n====== SHACL Validation Result ======");
        if (report.conforms()) {
            System.out.println("Real-time data DOES conform to the shapes (no violations).");
        } else {
            System.out.println("Real-time data has violations:");
            for (ReportEntry entry : report.getEntries()) {
                System.out.println(" - Message    : " + entry.message());
                System.out.println("   Focus Node : " + entry.focusNode());
                System.out.println("   Source     : " + entry.source());
                System.out.println();
            }
        }

        // 5) Also list which NodeShapes are violated vs. matched
        listViolatedAndMatchedShapes(shapesModel, report);
    }

    /**
     * This data should match s112 if s112 is defined as:
     *  - Heater=On, x <= 55
     *  - Homogenizer=Off, p <= 10
     */
    private static Model createDataModel() {
        Model data = ModelFactory.createDefaultModel();
        String pre = "https://anonymous.example.org#";

        // Heater: On, x=45
        Resource heater = data.createResource(pre + "Heater");
        heater.addProperty(RDF.type, data.createResource(pre + "Device"));
        heater.addProperty(data.createProperty(pre, "mode"), data.createResource(pre + "OnMode"));
        heater.addLiteral(data.createProperty(pre, "x"), 45);

        // Homogenizer: Off, p=9
        Resource homogenizer = data.createResource(pre + "Homogenizer");
        homogenizer.addProperty(RDF.type, data.createResource(pre + "Device"));
        homogenizer.addProperty(data.createProperty(pre, "mode"), data.createResource(pre + "OffMode"));
        homogenizer.addLiteral(data.createProperty(pre, "P"), 9);

        // realTimeState: references Heater, Homogenizer
        // typed as ex:State so that shapes with sh:targetClass ex:State will pick it up
        Resource realTimeState = data.createResource(pre + "realTimeState");
        realTimeState.addProperty(RDF.type, data.createResource(pre + "State"));
        realTimeState.addProperty(data.createProperty(pre, "hasHeater"), heater);
        realTimeState.addProperty(data.createProperty(pre, "hasHomogenizer"), homogenizer);

        return data;
    }

    /**
     * Lists out which NodeShapes are violated vs. matched, just for debug convenience.
     */
    private static void listViolatedAndMatchedShapes(Model shapesModel, ValidationReport report) {
        Set<Resource> allNodeShapes = gatherAllNodeShapes(shapesModel);

        // Build map: propertyShape -> nodeShape
        Map<Resource, Resource> propertyShapeToNodeShape = mapPropertyShapeToParent(shapesModel, allNodeShapes);

        // Gather violated shapes
        Set<Resource> violatedShapes = new HashSet<>();
        for (ReportEntry entry : report.getEntries()) {
            org.apache.jena.graph.Node src = entry.source();
            if (src == null) continue;

            Resource srcRes = shapesModel.wrapAsResource(src);
            if (allNodeShapes.contains(srcRes)) {
                violatedShapes.add(srcRes);
            } else {
                // maybe a propertyShape
                Resource parentNodeShape = propertyShapeToNodeShape.get(srcRes);
                if (parentNodeShape != null) {
                    violatedShapes.add(parentNodeShape);
                }
            }
        }

        // matched = all - violated
        Set<Resource> matchedShapes = new HashSet<>(allNodeShapes);
        matchedShapes.removeAll(violatedShapes);

        // print
        System.out.println("\n====== Shapes: violated or matched? ======");
        if (allNodeShapes.isEmpty()) {
            System.out.println("No NodeShape found in shapes model.");
        } else {
            System.out.println("All NodeShapes: " + allNodeShapes.size());
            System.out.println("Violated shapes: " + violatedShapes.size());
            for (Resource vs : violatedShapes) {
                System.out.println("  - " + vs.getURI());
            }
            System.out.println("Matched (non-violated) shapes: " + matchedShapes.size());
            for (Resource ms : matchedShapes) {
                System.out.println("  - " + ms.getURI());
            }
        }
        System.out.println("==============================================\n");
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
