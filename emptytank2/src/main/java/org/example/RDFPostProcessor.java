package org.example;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDFS;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * RDF Postprocessor for converting generic state relationships into more specific descriptive relationships.
 * This program reads an RDF file, processes state transition relationships, and writes the results to an output file.
 * Modified to work with Oven device states.
 *
 * Usage: Call RDFPostProcessor.processInferenceModel(infModel, outputPath) from your main application.
 */
public class RDFPostProcessor {
    // Updated to use anonymous namespace as in your RDF
    private static final String PRE = "http://anonymous.example.org#";
    private static final String EX_STATES = "http://example.org/states#";

    // Define original relationship predicates (only ModeChange and next exist in the data)
    private static final String MODE_CHANGE = EX_STATES + "ModeChange";
    private static final String NEXT = EX_STATES + "next";

    /**
     * Process the given inference model and create detailed relationships
     * @param model The inference model with the original relationships
     * @param outputPath The path where to save the processed RDF
     * @return The processed model with detailed relationships
     */
    public static Model processInferenceModel(Model model, String outputPath) {
        System.out.println("Starting RDF data processing for Oven system");

        try {
            System.out.println("Original model contains " + model.size() + " statements");

            // Create output model (initially containing all content from the original model)
            Model outputModel = ModelFactory.createDefaultModel();
            outputModel.add(model);
            outputModel.setNsPrefixes(model.getNsPrefixMap());

            // Process each relationship type (only ModeChange exists in the data)
            System.out.println("\nProcessing relationships...");
            processModeChangeRelationship(outputModel, MODE_CHANGE, "ModeChange");

            // If you also want to process 'next' relationships, uncomment the following line:
            // processModeChangeRelationship(outputModel, NEXT, "next");
            // Note: Based on your RDF, 'next' relationships don't seem to involve mode changes

            // Save the processed model if outputPath is provided
            if (outputPath != null && !outputPath.isEmpty()) {
                System.out.println("\nSaving processed model to: " + outputPath);
                try (FileOutputStream out = new FileOutputStream(outputPath)) {
                    RDFDataMgr.write(out, outputModel, RDFFormat.TURTLE_PRETTY);
                }
                System.out.println("Processing complete. Original model: " + model.size() +
                        " statements, Processed model: " + outputModel.size() + " statements");
            }

            // Show examples of results
            showExamples(outputModel);

            return outputModel;

        } catch (Exception e) {
            System.err.println("Error during processing: " + e.getMessage());
            e.printStackTrace();
            return model; // Return original model in case of error
        }
    }

    private static void processModeChangeRelationship(Model model, String originalPredicate, String relationshipName) {
        // Use simple SPARQL query to capture all relationships
        String relationQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX pre: <" + PRE + ">\n" +
                "SELECT ?subject ?object WHERE {\n" +
                "  ?subject <" + originalPredicate + "> ?object .\n" +
                "  ?subject rdf:type pre:State .\n" +
                "  ?object rdf:type pre:State .\n" +
                "}";

        System.out.println("Processing " + relationshipName + " relationships");

        // Collect all relationships to process
        Map<Resource, Set<Resource>> relationshipsToProcess = new HashMap<>();

        Query query = QueryFactory.create(relationQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.next();
                Resource subject = solution.getResource("subject");
                Resource object = solution.getResource("object");

                // Add relationship to the processing list
                if (!relationshipsToProcess.containsKey(subject)) {
                    relationshipsToProcess.put(subject, new HashSet<>());
                }
                relationshipsToProcess.get(subject).add(object);

                System.out.println("  Found relationship: " + subject.getLocalName() + " --(" + relationshipName + ")--> " + object.getLocalName());
            }
        }

        // Process all relationships and record statements to remove
        Set<Statement> statementsToRemove = new HashSet<>();
        int processedCount = 0;
        int notProcessedCount = 0;

        Property originalProperty = model.createProperty(originalPredicate);

        for (Map.Entry<Resource, Set<Resource>> entry : relationshipsToProcess.entrySet()) {
            Resource subject = entry.getKey();

            for (Resource object : entry.getValue()) {
                // Create detailed relationship
                boolean success = createDetailedRelationship(model, subject, object);

                if (success) {
                    // If successful, mark original statement for removal
                    Statement originalStatement = model.createStatement(subject, originalProperty, object);
                    statementsToRemove.add(originalStatement);
                    processedCount++;
                    System.out.println("  Successfully converted: " + subject.getLocalName() + " --(" + relationshipName + ")--> " + object.getLocalName());
                } else {
                    notProcessedCount++;
                    System.out.println("  Failed to convert: " + subject.getLocalName() + " --(" + relationshipName + ")--> " + object.getLocalName() + " (No device changes found)");
                }
            }
        }

        // Remove original statements that have been successfully converted
        for (Statement stmt : statementsToRemove) {
            model.remove(stmt);
        }

        System.out.println("  " + relationshipName + " statistics: Successfully converted " + processedCount +
                ", Failed to convert " + notProcessedCount + ", Removed " + statementsToRemove.size() + " original relationships");
    }

    private static boolean createDetailedRelationship(Model model, Resource state1, Resource state2) {
        // Modified query to work with Oven device
        String deviceInfoQuery =
                "PREFIX sh: <http://www.w3.org/ns/shacl#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX pre: <" + PRE + ">\n" +
                        "SELECT ?state ?deviceType ?device ?deviceMode WHERE {\n" +
                        "  {\n" +
                        "    ?state pre:hasOven ?device .\n" +
                        "    ?stateShape sh:property [\n" +
                        "      sh:path pre:hasOven ;\n" +
                        "      sh:node ?deviceShape\n" +
                        "    ] .\n" +
                        "    BIND(\"Oven\" as ?deviceType)\n" +
                        "  }\n" +
                        "  ?state rdf:type pre:State ;\n" +
                        "         pre:hasShape ?stateShape .\n" +
                        "  ?stateShape a sh:NodeShape .\n" +
                        "  ?deviceShape a sh:NodeShape ;\n" +
                        "    sh:property [ sh:path pre:mode ; sh:hasValue ?deviceMode ] .\n" +
                        "  FILTER(?state = <" + state1.getURI() + "> || ?state = <" + state2.getURI() + ">)\n" +
                        "}\n" +
                        "ORDER BY ?state ?deviceType";

        // Store device & mode information for each state - stateURI -> deviceType -> (device, mode)
        Map<String, Map<String, DeviceInfo>> stateDeviceInfo = new HashMap<>();
        stateDeviceInfo.put(state1.getURI(), new HashMap<>());
        stateDeviceInfo.put(state2.getURI(), new HashMap<>());

        Query query = QueryFactory.create(deviceInfoQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.next();
                Resource state = solution.getResource("state");
                String deviceType = solution.getLiteral("deviceType").getString();
                Resource device = solution.getResource("device");
                Resource deviceMode = solution.getResource("deviceMode");

                // Store device and mode information
                DeviceInfo info = new DeviceInfo(device, deviceMode);
                stateDeviceInfo.get(state.getURI()).put(deviceType, info);
            }
        }

        // Debug information
        System.out.println("    State " + state1.getLocalName() + " devices: " + stateDeviceInfo.get(state1.getURI()).keySet());
        System.out.println("    State " + state2.getLocalName() + " devices: " + stateDeviceInfo.get(state2.getURI()).keySet());

        // Check for device mode changes and device type changes
        Map<String, DeviceInfo> state1Devices = stateDeviceInfo.get(state1.getURI());
        Map<String, DeviceInfo> state2Devices = stateDeviceInfo.get(state2.getURI());

        // 1. First check for mode changes in common devices
        Map<String, ModeChangeInfo> deviceModeChanges = new LinkedHashMap<>(); // Maintain device order

        Set<String> commonDeviceTypes = new HashSet<>(state1Devices.keySet());
        commonDeviceTypes.retainAll(state2Devices.keySet());

        for (String deviceType : commonDeviceTypes) {
            DeviceInfo info1 = state1Devices.get(deviceType);
            DeviceInfo info2 = state2Devices.get(deviceType);

            if (!info1.mode.equals(info2.mode)) {
                String mode1Name = getLocalName(info1.mode.getURI());
                String mode2Name = getLocalName(info2.mode.getURI());

                deviceModeChanges.put(deviceType, new ModeChangeInfo(mode1Name, mode2Name));
                System.out.println("    Found device mode change: " + deviceType + " from " + mode1Name + " to " + mode2Name);
            }
        }

        // If we found mode changes in common devices, create a mode change relationship
        if (!deviceModeChanges.isEmpty()) {
            return createModeChangeRelationship(model, state1, state2, deviceModeChanges);
        }

        // 2. If no common device mode changes, check for device type changes
        if (!state1Devices.keySet().equals(state2Devices.keySet()) &&
                !state1Devices.isEmpty() && !state2Devices.isEmpty()) {

            return createDeviceTypeChangeRelationship(model, state1, state2, state1Devices, state2Devices);
        }

        System.out.println("    No device changes found");
        return false;
    }

    // Create a relationship for device mode changes
    private static boolean createModeChangeRelationship(Model model, Resource state1, Resource state2,
                                                        Map<String, ModeChangeInfo> deviceModeChanges) {
        StringBuilder predicateNameBuilder = new StringBuilder();
        StringBuilder labelBuilder = new StringBuilder("Change ");

        int changeCount = 0;
        for (Map.Entry<String, ModeChangeInfo> entry : deviceModeChanges.entrySet()) {
            String deviceType = entry.getKey();
            ModeChangeInfo modeInfo = entry.getValue();

            if (changeCount > 0) {
                predicateNameBuilder.append("_and_");
                labelBuilder.append(" and ");
            }

            predicateNameBuilder.append(deviceType).append("_")
                    .append(modeInfo.fromMode).append("_to_").append(modeInfo.toMode);

            labelBuilder.append(deviceType).append(" from ")
                    .append(modeInfo.fromMode).append(" to ").append(modeInfo.toMode);

            changeCount++;
        }

        // Create and add the new relationship triple
        String newPredicateName = predicateNameBuilder.toString();
        Property newPredicate = model.createProperty(EX_STATES, newPredicateName);
        model.add(state1, newPredicate, state2);

        // Add a label
        String labelString = labelBuilder.toString();
        model.add(newPredicate, RDFS.label, model.createLiteral(labelString));

        System.out.println("    Created mode change relationship: " + newPredicateName);
        System.out.println("    Label: " + labelString);

        return true;
    }

    // Create a relationship for device type changes, including mode information
    private static boolean createDeviceTypeChangeRelationship(Model model, Resource state1, Resource state2,
                                                              Map<String, DeviceInfo> state1Devices,
                                                              Map<String, DeviceInfo> state2Devices) {
        StringBuilder predicateNameBuilder = new StringBuilder();
        StringBuilder labelBuilder = new StringBuilder("Change from ");

        // Build relationship name and label
        // First state device information
        int deviceCount = 0;
        for (Map.Entry<String, DeviceInfo> entry : state1Devices.entrySet()) {
            String deviceType = entry.getKey();
            String modeName = getLocalName(entry.getValue().mode.getURI());

            if (deviceCount > 0) {
                predicateNameBuilder.append("_And_");
                labelBuilder.append(" and ");
            }

            predicateNameBuilder.append(deviceType).append("_").append(modeName);
            labelBuilder.append(deviceType).append("(").append(modeName).append(")");

            deviceCount++;
        }

        predicateNameBuilder.append("_To_");
        labelBuilder.append(" to ");

        // Second state device information
        deviceCount = 0;
        for (Map.Entry<String, DeviceInfo> entry : state2Devices.entrySet()) {
            String deviceType = entry.getKey();
            String modeName = getLocalName(entry.getValue().mode.getURI());

            if (deviceCount > 0) {
                predicateNameBuilder.append("_And_");
                labelBuilder.append(" and ");
            }

            predicateNameBuilder.append(deviceType).append("_").append(modeName);
            labelBuilder.append(deviceType).append("(").append(modeName).append(")");

            deviceCount++;
        }

        // Create relationship and label
        String predicateName = predicateNameBuilder.toString();
        Property newPredicate = model.createProperty(EX_STATES, predicateName);
        model.add(state1, newPredicate, state2);

        String labelString = labelBuilder.toString();
        model.add(newPredicate, RDFS.label, model.createLiteral(labelString));

        System.out.println("    Created device type change relationship: " + predicateName);
        System.out.println("    Label: " + labelString);

        return true;
    }

    // Helper class to store device and mode information
    private static class DeviceInfo {
        Resource device;
        Resource mode;

        DeviceInfo(Resource device, Resource mode) {
            this.device = device;
            this.mode = mode;
        }
    }

    // Helper class to store mode change information
    private static class ModeChangeInfo {
        String fromMode;
        String toMode;

        ModeChangeInfo(String fromMode, String toMode) {
            this.fromMode = fromMode;
            this.toMode = toMode;
        }
    }

    // Show example results
    private static void showExamples(Model model) {
        System.out.println("\nExamples of new specific relationships:");

        Property rdfsLabel = ResourceFactory.createProperty(RDFS.label.getURI());
        StmtIterator labelIter = model.listStatements(null, rdfsLabel, (RDFNode)null);

        int exampleCount = 0;
        while (labelIter.hasNext() && exampleCount < 5) {
            Statement labelStmt = labelIter.next();
            Resource predicate = labelStmt.getSubject();

            // Only show our custom device-related predicates
            if (predicate.getURI().startsWith(EX_STATES) &&
                    (predicate.getURI().contains("_to_") || predicate.getURI().contains("_To_")) &&
                    !predicate.getURI().contains("#s")) {  // Avoid showing generic state transitions

                // Find statements using this predicate
                StmtIterator predIter = model.listStatements(null, ResourceFactory.createProperty(predicate.getURI()), (RDFNode)null);
                if (predIter.hasNext()) {
                    Statement usage = predIter.next();
                    System.out.println(usage.getSubject().getLocalName() + " " +
                            getLocalName(predicate.getURI()) + " " +
                            usage.getObject().asResource().getLocalName() +
                            " (" + labelStmt.getObject().toString() + ")");
                    exampleCount++;
                }
            }
        }

        // Check if any original relationships remain
        System.out.println("\nChecking for remaining original relationships:");
        String[] originalRelations = {MODE_CHANGE, NEXT};

        for (String rel : originalRelations) {
            Property p = model.createProperty(rel);
            StmtIterator iter = model.listStatements(null, p, (RDFNode)null);
            int count = 0;
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                count++;
                if (count <= 3) { // Only show first three examples
                    System.out.println("  " + stmt.getSubject().getLocalName() + " " +
                            getLocalName(rel) + " " +
                            stmt.getObject().asResource().getLocalName());
                }
            }
            System.out.println("  Total " + getLocalName(rel) + ": " + count + " relationships" +
                    (count > 3 ? " (only showing first 3)" : ""));
        }
    }

    // From URI to local name
    private static String getLocalName(String uri) {
        int lastHash = uri.lastIndexOf('#');
        if (lastHash != -1) {
            return uri.substring(lastHash + 1);
        }
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash != -1) {
            return uri.substring(lastSlash + 1);
        }
        return uri;
    }
}