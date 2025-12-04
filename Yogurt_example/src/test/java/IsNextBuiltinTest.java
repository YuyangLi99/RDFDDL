package org.example;

import static org.junit.Assert.fail;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.BuiltinRegistry;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;


import org.apache.jena.shacl.*;

/**
 * keep Yogurt, Heater
 *      dynamicly add State + SHACL NodeShape
 */
public class IsNextBuiltinTest {
    @Test
    //public void main(String[] args) {
    public void main() {

        //=================================================
        // 1) create Dataset + default model
        //=================================================

        Dataset dataset = DatasetFactory.create();
        Model model = dataset.getDefaultModel();

        String example = "http://example.org/states#";

//        String nextpredicate = "<http://example.org/states#next>";

        String ModeChange = "<http://example.org/states#ModeChange>";
        String ModeChange2 = "<http://example.org/states#ModeChange2>";
        String ModeChange3 = "<http://example.org/states#ModeChange3>";
        String ModeChange4 = "<http://example.org/states#ModeChange4>";

        model.setNsPrefix("ex", example);
        Property nextpredicate = model.createProperty(example, "next");

        String pre = "https://anonymous.example.org#";
        model.setNsPrefix("ex", pre);
        String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
        String schema = "http://schema.org/";
        String foaf = "http://xmlns.com/foaf/0.1/";
        model.setNsPrefix("ex", schema);
        model.setNsPrefix("foaf", foaf);
        model.setNsPrefix("xsd", XSD.getURI());
        model.setNsPrefix("pre",pre);
        model.setNsPrefix("rdf", rdf);
        model.setNsPrefix("rdfs", rdfs);

        // normal Property
        Property a = model.createProperty(rdf, "type");
//        Property subClassOf = model.createProperty(rdfs, "subClassOf");
//        Property Class =model.createProperty(rdfs, "Class");
        Property address = model.createProperty(schema, "address");
        Property manufacturer = model.createProperty(schema, "manufacturer");
        Property modelNumber = model.createProperty(schema, "model");
        Property capacity = model.createProperty(schema, "capacity");

        Property temperatureRange = model.createProperty(pre, "temperatureRange");
        Property pressureRange = model.createProperty(pre, "pressureRange");
        Property hasProcessStep  = model.createProperty(pre, "hasProcessStep");
        Property usesEquipment   = model.createProperty(pre, "usesEquipment");
        Property inputMaterial   = model.createProperty(pre, "inputMaterial");
        Property outputMaterial  = model.createProperty(pre, "outputMaterial");
        Property nextStep        = model.createProperty(pre, "nextStep");
        Property name            = model.createProperty(pre, "name");
        Property mode             = model.createProperty(pre, "mode");

        Property hasMode = model.createProperty(pre, "hasMode");
        Property hasDevice = model.createProperty(pre, "hasDevice");
        Property hasODE = model.createProperty(pre, "hasODE");

        // ode property
        Property derivative   = model.createProperty(pre + "derivative");
        Property evolvingVariable = model.createProperty(pre + "evolvingVariable");
        Property startingCondition = model.createProperty(pre + "startingCondition");
        Property endingCondition   = model.createProperty(pre + "endingCondition");
        Property evolutionDomainConstraint = model.createProperty(pre, "evolutionDomainConstraint");

        Property x = model.createProperty(pre + "x"); // variable x
        Property P = model.createProperty(pre + "P"); // variable p

        Property hasHeater = model.createProperty(pre, "hasHeater");
        Property hasHomogenizer = model.createProperty(pre, "hasHomogenizer");
        Property hasShape = model.createProperty(pre, "hasShape");
        Property hasDeviceMode = model.createProperty(pre, "hasDeviceMode");

        // NEW PROPERTIES
        Property initialState = model.createProperty(pre, "initialState");
        Property goalState = model.createProperty(pre, "goalState");
        Property hasButton = model.createProperty(pre, "hasButton");
        Property serviceTechnician = model.createProperty(pre, "serviceTechnician");
        Property label = model.createProperty(pre, "label");
        Property triggerTransition = model.createProperty(pre, "trigger_transition");
        Property forDevice = model.createProperty(pre, "forDevice");
        Property jobTitle = model.createProperty(schema, "jobTitle");
        Property worksFor = model.createProperty(schema, "worksFor");
        Property operatesProcess = model.createProperty(pre, "operatesProcess");
        Property operatesEquipment = model.createProperty(pre, "operatesEquipment");
        Property hasEquipment = model.createProperty(pre, "hasEquipment");

        Property hasPossibleModeProp = model.createProperty(pre, "hasPossibleMode");

        // device Resource
        Resource Heater          = model.createResource(pre + "Heater");
        Resource Homogenizer     = model.createResource(pre + "Homogenizer");
        Resource Pasteurizer     = model.createResource(pre + "Pasteurizer");
        Resource Cooler          = model.createResource(pre + "Cooler");
        Resource Device          = model.createResource(pre + "Device");

        Resource ModeRecord      = model.createResource(pre + "ModeRecord");

        Resource onMode = model.createResource(pre + "OnMode");
        Resource offMode = model.createResource(pre + "OffMode");

        Resource ODE = model.createResource(pre + "ODE");

        // NEW RESOURCES
        Resource Button = model.createResource(pre + "Button");
        // FOAF properties - define before using them
        Property foafName = model.createProperty(foaf, "name");
        Resource foafPerson = model.createResource(foaf + "Person");

        //Heater OnMode
        Resource HeaterOnMode = model.createResource(pre + "HeaterOnMode");
        HeaterOnMode.addProperty(RDF.type, ModeRecord);
        HeaterOnMode.addProperty(hasMode, onMode);
        HeaterOnMode.addProperty(hasDevice, Heater);
        Resource HeaterOnODE = model.createResource();
        HeaterOnODE.addProperty(RDF.type, model.createResource(pre + "ODE"));
        HeaterOnODE.addProperty(derivative, "9.68");
        HeaterOnODE.addProperty(startingCondition, "x<=55");
        HeaterOnODE.addProperty(endingCondition, "x>=55");
        HeaterOnODE.addProperty(evolutionDomainConstraint, "x<=65");
        HeaterOnODE.addProperty(evolvingVariable, model.createResource(pre + "x"));
        HeaterOnMode.addProperty(hasODE, HeaterOnODE);

        // Heater OffMode
        Resource HeaterOffMode = model.createResource(pre + "HeaterOffMode");
        HeaterOffMode.addProperty(RDF.type, ModeRecord);
        HeaterOffMode.addProperty(hasMode, offMode);
        HeaterOffMode.addProperty(hasDevice, Heater);
        Resource HeaterOffODE = model.createResource();
        HeaterOffODE.addProperty(RDF.type, ODE);
        HeaterOffODE.addProperty(derivative, "-9.68");
        HeaterOffODE.addProperty(startingCondition, "x>=55");
        HeaterOffODE.addProperty(endingCondition, "false");
        HeaterOffODE.addProperty(evolutionDomainConstraint, "x<=65");
        HeaterOffODE.addProperty(evolvingVariable, model.createResource(pre + "x"));
        HeaterOffMode.addProperty(hasODE, HeaterOffODE);

        //  Heater, Homogenizer information
        Heater.addProperty(name, "Industrial Heater");
        Heater.addProperty(manufacturer, "Heater Manufacturer Inc.");
        Heater.addProperty(modelNumber, "IH-5000");
        Heater.addProperty(capacity, "5000L");
        Heater.addProperty(temperatureRange, "Up to 100°C");

        // NEW - Add button and technician to Heater
        Resource HeaterStartBtn = model.createResource(pre + "HeaterStartBtn");
        HeaterStartBtn.addProperty(RDF.type, Button);
        HeaterStartBtn.addProperty(label, "Heater START");
        HeaterStartBtn.addProperty(triggerTransition, model.createResource("http://example.org/states#Heater_OnMode_to_OffMode_and_Homogenizer_OnMode_to_OffMode"));
        HeaterStartBtn.addProperty(forDevice, Heater);

        Resource TechAlice = model.createResource(pre + "TechAlice");
        TechAlice.addProperty(RDF.type, foafPerson);
        TechAlice.addProperty(foafName, "Alice Müller");
        TechAlice.addProperty(jobTitle, "Senior Service Technician");
        TechAlice.addProperty(worksFor, model.createResource(pre + "YogurtFactory"));

        Heater.addProperty(hasButton, HeaterStartBtn);
        Heater.addProperty(serviceTechnician, TechAlice);

        Resource HomogenizerOnMode = model.createResource(pre + "HomogenizerOnMode");
        HomogenizerOnMode.addProperty(RDF.type, ModeRecord);
        HomogenizerOnMode.addProperty(hasMode, onMode);
        HomogenizerOnMode.addProperty(hasDevice, Homogenizer);
        Resource HomogenizerOnODE = model.createResource();
        HomogenizerOnODE.addProperty(RDF.type, ODE);
        HomogenizerOnODE.addProperty(derivative, "2");
        HomogenizerOnODE.addProperty(startingCondition, "x>=55");
        HomogenizerOnODE.addProperty(endingCondition, "P>=10");
        HomogenizerOnODE.addProperty(evolutionDomainConstraint, "P<=20");
        HomogenizerOnODE.addProperty(evolvingVariable, model.createResource(pre + "P"));
        HomogenizerOnMode.addProperty(hasODE, HomogenizerOnODE);

        Resource HomogenizerOffMode = model.createResource(pre + "HomogenizerOffMode");
        HomogenizerOffMode.addProperty(RDF.type, ModeRecord);
        HomogenizerOffMode.addProperty(hasMode, offMode);
        HomogenizerOffMode.addProperty(hasDevice, Homogenizer);
        Resource HomogenizerOffODE = model.createResource();
        HomogenizerOffODE.addProperty(RDF.type, ODE);
        HomogenizerOffODE.addProperty(derivative, "-2");
        HomogenizerOffODE.addProperty(startingCondition, "P>=10");
        HomogenizerOffODE.addProperty(endingCondition, "false");
        HomogenizerOffODE.addProperty(evolutionDomainConstraint, "P<=20");
        HomogenizerOffODE.addProperty(evolvingVariable, model.createResource(pre + "P"));
        HomogenizerOffMode.addProperty(hasODE, HomogenizerOffODE);

        Homogenizer.addProperty(name, "High-Pressure Homogenizer");
        Homogenizer.addProperty(manufacturer, "Homogenizer Co.");
        Homogenizer.addProperty(modelNumber, "HPH-2000");
        Homogenizer.addProperty(capacity, "2000L");
        Homogenizer.addProperty(pressureRange, "Up to 300 bar");
        Homogenizer.addProperty(a, Device);

        Resource PasteurizationOnMode = model.createResource(pre + "PasteurizationOnMode");
        PasteurizationOnMode.addProperty(RDF.type, ModeRecord);
        PasteurizationOnMode.addProperty(hasMode, onMode);
        PasteurizationOnMode.addProperty(hasDevice, Pasteurizer);
        Resource PasteurizationOnODE = model.createResource();
        PasteurizationOnODE.addProperty(RDF.type, ODE);
        PasteurizationOnODE.addProperty(derivative, "9.68");
        PasteurizationOnODE.addProperty(startingCondition, "x <= 90");
        PasteurizationOnODE.addProperty(endingCondition, "x >= 90");
        PasteurizationOnODE.addProperty(evolutionDomainConstraint, "x <= 95");
        PasteurizationOnODE.addProperty(evolvingVariable, model.createResource(pre + "x"));
        PasteurizationOnMode.addProperty(hasODE, PasteurizationOnODE);

        // Pasteurization mode -- off
        Resource PasteurizationOffMode = model.createResource(pre + "PasteurizationOffMode");
        PasteurizationOffMode.addProperty(RDF.type, ModeRecord);
        PasteurizationOffMode.addProperty(hasMode, offMode);
        PasteurizationOffMode.addProperty(hasDevice, Pasteurizer);
        Resource PasteurizationOffODE = model.createResource();
        PasteurizationOffODE.addProperty(RDF.type, ODE);
        PasteurizationOffODE.addProperty(derivative, "-9.68");
        PasteurizationOffODE.addProperty(startingCondition, "x >= 90");
        PasteurizationOffODE.addProperty(endingCondition, "false");
        PasteurizationOffODE.addProperty(evolutionDomainConstraint, "x <= 95");
        PasteurizationOffODE.addProperty(evolvingVariable, model.createResource(pre + "x"));
        PasteurizationOffMode.addProperty(hasODE, PasteurizationOffODE);

        Pasteurizer.addProperty(name, "Industrial Pasteurizer");
        Pasteurizer.addProperty(manufacturer, "Pasteurizer Corp.");
        Pasteurizer.addProperty(modelNumber, "IP-3000");
        Pasteurizer.addProperty(capacity, "3000L");

        Resource CoolerOnMode = model.createResource(pre + "CoolerOnMode");
        CoolerOnMode.addProperty(RDF.type, ModeRecord);
        CoolerOnMode.addProperty(hasMode, onMode);
        CoolerOnMode.addProperty(hasDevice, Cooler);
        Resource CoolerOnODE = model.createResource();
        CoolerOnODE.addProperty(RDF.type, ODE);
        CoolerOnODE.addProperty(derivative, "-9.68");
        CoolerOnODE.addProperty(startingCondition, "x >= 90");
        CoolerOnODE.addProperty(endingCondition, "x <= 40");
        CoolerOnODE.addProperty(evolutionDomainConstraint, "x <= 90");
        CoolerOnODE.addProperty(evolvingVariable, model.createResource(pre + "x"));
        CoolerOnMode.addProperty(hasODE, CoolerOnODE);

        Resource CoolerOffMode = model.createResource(pre + "CoolerOffMode");
        CoolerOffMode.addProperty(RDF.type, ModeRecord);
        CoolerOffMode.addProperty(hasMode, offMode);
        CoolerOffMode.addProperty(hasDevice, Cooler);
        Resource CoolerOffODE = model.createResource();
        CoolerOffODE.addProperty(RDF.type, ODE);
        CoolerOffODE.addProperty(derivative, "9.68");
        CoolerOffODE.addProperty(startingCondition, "x <= 40");
        CoolerOffODE.addProperty(endingCondition, "false");
        CoolerOffODE.addProperty(evolutionDomainConstraint, "x <= 45");
        CoolerOffODE.addProperty(evolvingVariable, model.createResource(pre + "x"));
        CoolerOffMode.addProperty(hasODE, CoolerOffODE);

        Cooler.addProperty(name, "Industrial Cooler");
        Cooler.addProperty(manufacturer, "Cooler Solutions Inc.");
        Cooler.addProperty(modelNumber, "IC-1500");
        Cooler.addProperty(capacity, "1500L");

        // factory
        Resource yogurtFactory = model.createResource(pre + "YogurtFactory");
        yogurtFactory.addProperty(a, model.createResource(schema + "Organization"));
        yogurtFactory.addProperty(name, "Yogurt Production Ltd.");
        yogurtFactory.addProperty(address, "123 Dairy Lane, Milk City, Country");
        yogurtFactory.addProperty(hasEquipment, Heater);
        yogurtFactory.addProperty(hasEquipment, Homogenizer);

        // Process
        Resource yogurtProduction = model.createResource(pre + "YogurtProductionProcess");
        yogurtProduction.addProperty(a, model.createResource(schema + "Process"));
        yogurtProduction.addProperty(name, "Yogurt Production Process");
        yogurtFactory.addProperty(operatesProcess, yogurtProduction);

        // manufacturing steps
        Resource heatingStep = model.createResource(pre + "HeatingStep");
        heatingStep.addProperty(a, model.createResource(schema + "ProcessStep"));
        heatingStep.addProperty(name, "Heating Milk");
        heatingStep.addProperty(usesEquipment, Heater);
        heatingStep.addProperty(inputMaterial, model.createResource(pre + "StandardMilk"));
        heatingStep.addProperty(outputMaterial, model.createResource(pre + "HeatedMilk"));

        Resource homogenizationStep = model.createResource(pre + "HomogenizationStep");
        homogenizationStep.addProperty(a, model.createResource(schema + "ProcessStep"));
        homogenizationStep.addProperty(name, "Homogenizing Milk");
        homogenizationStep.addProperty(usesEquipment, Homogenizer);
        homogenizationStep.addProperty(inputMaterial, model.createResource(pre + "HeatedMilk"));
        homogenizationStep.addProperty(outputMaterial, model.createResource(pre + "HomogenizedMilk"));

        heatingStep.addProperty(nextStep, homogenizationStep);
        yogurtProduction.addProperty(hasProcessStep, heatingStep);
        yogurtProduction.addProperty(hasProcessStep, homogenizationStep);

        // product/materials
        Resource standardMilk = model.createResource(pre + "StandardMilk");
        standardMilk.addProperty(a, model.createResource(schema + "Product"));
        standardMilk.addProperty(name, "Standard Milk");

        Resource heatedMilk = model.createResource(pre + "HeatedMilk");
        heatedMilk.addProperty(a, model.createResource(schema + "Product"));
        heatedMilk.addProperty(name, "Heated Milk");

        Resource homogenizedMilk = model.createResource(pre + "HomogenizedMilk");
        homogenizedMilk.addProperty(a, model.createResource(schema + "Product"));
        homogenizedMilk.addProperty(name, "Homogenized Milk");

        // Yogurt + QualityControl
        Resource yogurtProduct = model.createResource(pre + "Yogurt");
        yogurtProduct.addProperty(a, model.createResource(schema + "Product"));
        yogurtProduct.addProperty(name, "Yogurt");

        Resource qualityControl = model.createResource(pre + "QualityControl");
        qualityControl.addProperty(a, model.createResource(schema + "Process"));
        qualityControl.addProperty(name, "Quality Control Step");
        qualityControl.addProperty(inputMaterial, yogurtProduct);
        Resource approvedYogurt = model.createResource(pre + "ApprovedYogurt");
        approvedYogurt.addProperty(a, model.createResource(schema + "Product"));
        approvedYogurt.addProperty(name, "Approved Yogurt");
        qualityControl.addProperty(outputMaterial, approvedYogurt);
        homogenizationStep.addProperty(nextStep, qualityControl);

        // packaging
        Resource packagingStep = model.createResource(pre + "PackagingStep");
        packagingStep.addProperty(a, model.createResource(schema + "ProcessStep"));
        packagingStep.addProperty(name, "Packaging Yogurt");
        packagingStep.addProperty(inputMaterial, approvedYogurt);
        Resource packagedYogurt = model.createResource(pre + "PackagedYogurt");
        packagedYogurt.addProperty(a, model.createResource(schema + "Product"));
        packagedYogurt.addProperty(name, "Packaged Yogurt");
        packagingStep.addProperty(outputMaterial, packagedYogurt);
        qualityControl.addProperty(nextStep, packagingStep);

        // Add to the process
        yogurtProduction.addProperty(hasProcessStep, qualityControl);
        yogurtProduction.addProperty(hasProcessStep, packagingStep);

        // Person
        Resource operator = model.createResource(pre + "Operator");
        operator.addProperty(a, model.createResource(schema + "Person"));
        operator.addProperty(name, "John Doe");
        operator.addProperty(operatesEquipment, Heater);
        operator.addProperty(operatesEquipment, Homogenizer);

        String shNS = "http://www.w3.org/ns/shacl#";

        model.setNsPrefix("sh", shNS);
        Property shPath = model.createProperty(shNS, "path");
        Property shMinCount = model.createProperty(shNS, "minCount");
        Property shMaxCount = model.createProperty(shNS, "maxCount");
        Property shHasValue = model.createProperty(shNS, "hasValue");
        Property shMinInclusive = model.createProperty(shNS, "minInclusive");
        Property shMaxInclusive = model.createProperty(shNS, "maxInclusive");
        Property shProperty = model.createProperty(shNS, "property");
        Property shTargetClass = model.createProperty(shNS, "targetClass");
        Property shNode = model.createProperty(shNS, "node");

        //Create Resource
        Resource State = model.createResource(pre + "State");

        // NEW - Add property definitions for initialState and goalState
        initialState.addProperty(RDF.type, RDF.Property);
        initialState.addProperty(model.createProperty(rdfs, "domain"), model.createResource(schema + "Process"));
        initialState.addProperty(model.createProperty(rdfs, "range"), State);

        goalState.addProperty(RDF.type, RDF.Property);
        goalState.addProperty(model.createProperty(rdfs, "domain"), model.createResource(schema + "Process"));
        goalState.addProperty(model.createProperty(rdfs, "range"), State);

        // state is defined as ：
        // ex:s112 a ex:State ;
        //   ex:hasHeater ex:Heater ;
        //   ex:hasHomogenizer ex:Homogenizer ;
        //   ex:hasShape ex:s112Shape .
        Resource s112 = model.createResource(pre + "s112");
        s112.addProperty(RDF.type, State);
        s112.addProperty(hasHeater, Heater);
        s112.addProperty(hasHomogenizer, Homogenizer);
        s112.addProperty(hasShape, model.createResource(pre + "s112Shape"));

        // NEW - Add initial and goal states to the process
        yogurtProduction.addProperty(initialState, s112);

        // --- s112: Heater=On, Homogenizer=Off, x<=55, p<=10

        // ex:s112HeaterShape ：
        // ex:s112HeaterShape a sh:NodeShape ;
        //   sh:property [ sh:path ex:mode ; sh:minCount 1 ; sh:maxCount 1 ; sh:hasValue ex:OffMode ],
        //               [ sh:path ex:x ; sh:minCount 1 ; sh:maxCount 1 ; sh:maxInclusive 55 ; ] .
        Resource s112HeaterShape = model.createResource(pre + "s112HeaterShape");
        s112HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

        Resource heaterShapePropMode_s112 = model.createResource();
        heaterShapePropMode_s112.addProperty(shPath, mode);
        heaterShapePropMode_s112.addLiteral(shMinCount, 1);
        heaterShapePropMode_s112.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s112.addProperty(shHasValue, onMode);

        Resource heaterShapePropX_s112 = model.createResource();
        heaterShapePropX_s112.addProperty(shPath, x);
        heaterShapePropX_s112.addLiteral(shMinCount, 1);
        heaterShapePropX_s112.addLiteral(shMaxCount, 1);
        heaterShapePropX_s112.addLiteral(shMaxInclusive, model.createTypedLiteral(55));

        s112HeaterShape.addProperty(shProperty, heaterShapePropMode_s112);
        s112HeaterShape.addProperty(shProperty, heaterShapePropX_s112);

        // ex:s112HomogenizerShape：
        // ex:s112HomogenizerShape a sh:NodeShape ;
        //   sh:property [ sh:path ex:mode ; sh:minCount 1 ; sh:maxCount 1 ; sh:hasValue ex:OnMode ],
        //               [ sh:path ex:p ; sh:minCount 1 ; sh:maxCount 1 ; sh:maxInclusive 10 ] .
        Resource s112HomogenizerShape = model.createResource(pre + "s112HomogenizerShape");
        s112HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

        Resource homogenizerShapePropMode_s112 = model.createResource();
        homogenizerShapePropMode_s112.addProperty(shPath, mode);
        homogenizerShapePropMode_s112.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s112.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s112.addProperty(shHasValue, offMode);

        Resource homogenizerShapePropP_s112 = model.createResource();
        homogenizerShapePropP_s112.addProperty(shPath, P);
        homogenizerShapePropP_s112.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s112.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s112.addLiteral(shMaxInclusive, model.createTypedLiteral(10));

        s112HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s112);
        s112HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s112);

        // ex:s112Shape，
        // ex:s112Shape a sh:NodeShape ;
        //   sh:targetClass ex:State ;
        //   sh:property [ sh:path ex:hasHeater ; sh:minCount 1 ; sh:maxCount 1 ; sh:node ex:s112HeaterShape ],
        //               [ sh:path ex:hasHomogenizer ; sh:minCount 1 ; sh:maxCount 1 ; sh:node ex:s112HomogenizerShape ] .

        Resource s112Shape = model.createResource(pre + "s112Shape");
        s112Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s112Shape.addProperty(shTargetClass,State);

        Resource stateHeaterShapeProp_s112 = model.createResource();
        stateHeaterShapeProp_s112.addProperty(shPath, hasHeater);
        stateHeaterShapeProp_s112.addLiteral(shMinCount, 1);
        stateHeaterShapeProp_s112.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp_s112.addProperty(shNode, s112HeaterShape);

        Resource stateHomogenizerShapeProp_s112 = model.createResource();
        stateHomogenizerShapeProp_s112.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp_s112.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp_s112.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp_s112.addProperty(shNode, s112HomogenizerShape);

        s112Shape.addProperty(shProperty, stateHeaterShapeProp_s112);
        s112Shape.addProperty(shProperty, stateHomogenizerShapeProp_s112);

        //=================================================
        // create state s122
        // Heater=Off, Homogenizer=Off
        // x<=55, p<=10
        //=================================================

        // ex:s122 a ex:State ;
        //    ex:hasHeater ex:Heater ;
        //    ex:hasHomogenizer ex:Homogenizer ;
        //    ex:hasShape ex:s122Shape .
        Resource s122 = model.createResource(pre + "s122");
        s122.addProperty(RDF.type, State);
        s122.addProperty(hasHeater, Heater);
        s122.addProperty(hasHomogenizer, Homogenizer);
        s122.addProperty(hasShape, model.createResource(pre + "s122Shape"));

        // --- s122: Heater=Off, Homogenizer=Off, x<=55, p<=10

// ex:s122HeaterShape ：
// ex:s122HeaterShape a sh:NodeShape ;
//   sh:property [ sh:path ex:mode ; sh:minCount 1 ; sh:maxCount 1 ; sh:hasValue ex:OffMode ],
//               [ sh:path ex:x ; sh:minCount 1 ; sh:maxCount 1 ; sh:maxInclusive 55 ; ] .
        Resource s122HeaterShape = model.createResource(pre + "s122HeaterShape");
        s122HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

        Resource heaterShapePropMode_s122 = model.createResource();
        heaterShapePropMode_s122.addProperty(shPath, mode);
        heaterShapePropMode_s122.addLiteral(shMinCount, 1);
        heaterShapePropMode_s122.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s122.addProperty(shHasValue, offMode);

        Resource heaterShapePropX_s122 = model.createResource();
        heaterShapePropX_s122.addProperty(shPath, x);
        heaterShapePropX_s122.addLiteral(shMinCount, 1);
        heaterShapePropX_s122.addLiteral(shMaxCount, 1);
        heaterShapePropX_s122.addLiteral(shMaxInclusive, model.createTypedLiteral(55));

        s122HeaterShape.addProperty(shProperty, heaterShapePropMode_s122);
        s122HeaterShape.addProperty(shProperty, heaterShapePropX_s122);

// ex:s122HomogenizerShape：
// ex:s122HomogenizerShape a sh:NodeShape ;
//   sh:property [ sh:path ex:mode ; sh:minCount 1 ; sh:maxCount 1 ; sh:hasValue ex:OffMode ],
//               [ sh:path ex:p ; sh:minCount 1 ; sh:maxCount 1 ; sh:maxInclusive 10 ] .
        Resource s122HomogenizerShape = model.createResource(pre + "s122HomogenizerShape");
        s122HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

        Resource homogenizerShapePropMode_s122 = model.createResource();
        homogenizerShapePropMode_s122.addProperty(shPath, mode);
        homogenizerShapePropMode_s122.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s122.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s122.addProperty(shHasValue, offMode);

        Resource homogenizerShapePropP_s122 = model.createResource();
        homogenizerShapePropP_s122.addProperty(shPath, P);
        homogenizerShapePropP_s122.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s122.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s122.addLiteral(shMaxInclusive, model.createTypedLiteral(10));

        s122HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s122);
        s122HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s122);

// ex:s122Shape，
// ex:s122Shape a sh:NodeShape ;
//   sh:targetClass ex:State ;
//   sh:property [ sh:path ex:hasHeater ; sh:minCount 1 ; sh:maxCount 1 ; sh:node ex:s122HeaterShape ],
//               [ sh:path ex:hasHomogenizer ; sh:minCount 1 ; sh:maxCount 1 ; sh:node ex:s122HomogenizerShape ] .
        Resource s122Shape = model.createResource(pre + "s122Shape");
        s122Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s122Shape.addProperty(shTargetClass, State);

        Resource stateHeaterShapeProp = model.createResource();
        stateHeaterShapeProp.addProperty(shPath, hasHeater);
        stateHeaterShapeProp.addLiteral(shMinCount, 1);
        stateHeaterShapeProp.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp.addProperty(shNode, s122HeaterShape);
        Resource stateHomogenizerShapeProp = model.createResource();
        stateHomogenizerShapeProp.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp.addProperty(shNode, s122HomogenizerShape);
        s122Shape.addProperty(shProperty, stateHeaterShapeProp);
        s122Shape.addProperty(shProperty, stateHomogenizerShapeProp);

        //=================================================
        // create state s211，
        // --- s211: Heater=On, Homogenizer=On, x>=55, p<=10
        //=================================================

        // ex:s211 a ex:State ;
//   ex:hasHeater ex:Heater ;
//   ex:hasHomogenizer ex:Homogenizer ;
//   ex:hasShape ex:s211Shape .
        Resource s211 = model.createResource(pre + "s211");
        s211.addProperty(RDF.type, State);
        s211.addProperty(hasHeater, Heater);
        s211.addProperty(hasHomogenizer, Homogenizer);
        s211.addProperty(hasShape, model.createResource(pre + "s211Shape"));

        // --- s211: Heater=On, Homogenizer=On, x>=55, p<=10

// 2) ex:s211HeaterShape：
// ex:s211HeaterShape a sh:NodeShape ;
//   sh:property [ sh:path ex:mode; sh:minCount 1; sh:maxCount 1; sh:hasValue ex:OnMode ],
//               [ sh:path ex:x; sh:minCount 1; sh:maxCount 1; sh:minInclusive 55 ] .
        Resource s211HeaterShape = model.createResource(pre + "s211HeaterShape");
        s211HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

        // heater mode On
        Resource heaterShapePropMode_s211 = model.createResource();
        heaterShapePropMode_s211.addProperty(shPath, mode);
        heaterShapePropMode_s211.addLiteral(shMinCount, 1);
        heaterShapePropMode_s211.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s211.addProperty(shHasValue, onMode);

// heater x (x >= 55)
        Resource heaterShapePropX_s211 = model.createResource();
        heaterShapePropX_s211.addProperty(shPath, x);
        heaterShapePropX_s211.addLiteral(shMinCount, 1);
        heaterShapePropX_s211.addLiteral(shMaxCount, 1);
        heaterShapePropX_s211.addLiteral(shMinInclusive, 55);

// add to s211HeaterShape
        s211HeaterShape.addProperty(shProperty, heaterShapePropMode_s211);
        s211HeaterShape.addProperty(shProperty, heaterShapePropX_s211);

// 3) ex:s211HomogenizerShape：
// ex:s211HomogenizerShape a sh:NodeShape ;
//   sh:property [ sh:path ex:mode; sh:minCount 1; sh:maxCount 1; sh:hasValue ex:OnMode ],
//               [ sh:path ex:p; sh:minCount 1; sh:maxCount 1; sh:maxInclusive 10 ] .
        Resource s211HomogenizerShape = model.createResource(pre + "s211HomogenizerShape");
        s211HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// homogenizer mode ON
        Resource homogenizerShapePropMode_s211 = model.createResource();
        homogenizerShapePropMode_s211.addProperty(shPath, mode);
        homogenizerShapePropMode_s211.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s211.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s211.addProperty(shHasValue, onMode);

// homogenizer (p <= 10)
        Resource homogenizerShapePropP_s211 = model.createResource();
        homogenizerShapePropP_s211.addProperty(shPath, P);
        homogenizerShapePropP_s211.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s211.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s211.addLiteral(shMaxInclusive, 10);

// add to s211HomogenizerShape
        s211HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s211);
        s211HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s211);

// 4) ex:s211Shape：
// ex:s211Shape a sh:NodeShape ;
//   sh:targetClass ex:State ;
//   sh:property [ sh:path ex:hasHeater; sh:minCount 1; sh:maxCount 1; sh:node ex:s211HeaterShape ],
//               [ sh:path ex:hasHomogenizer; sh:minCount 1; sh:maxCount 1; sh:node ex:s211HomogenizerShape ] .
        Resource s211Shape = model.createResource(pre + "s211Shape");
        s211Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s211Shape.addProperty(shTargetClass, State);

// stateHeaterShapeProp_s211
        Resource stateHeaterShapeProp_s211 = model.createResource();
        stateHeaterShapeProp_s211.addProperty(shPath, hasHeater);
        stateHeaterShapeProp_s211.addLiteral(shMinCount, 1);
        stateHeaterShapeProp_s211.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp_s211.addProperty(shNode, s211HeaterShape);

// stateHomogenizerShapeProp_s211
        Resource stateHomogenizerShapeProp_s211 = model.createResource();
        stateHomogenizerShapeProp_s211.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp_s211.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp_s211.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp_s211.addProperty(shNode, s211HomogenizerShape);

        s211Shape.addProperty(shProperty, stateHeaterShapeProp_s211);
        s211Shape.addProperty(shProperty, stateHomogenizerShapeProp_s211);

        //=================================================
        // create state s212
        // Heater=On, Homogenizer=Off
        // x>=55, p<=10
        //=================================================

        // ex:s212 a ex:State ;
//   ex:hasHeater ex:Heater ;
//   ex:hasHomogenizer ex:Homogenizer ;
//   ex:hasShape ex:s212Shape .
        Resource s212 = model.createResource(pre + "s212");
        s212.addProperty(RDF.type, State);
        s212.addProperty(hasHeater, Heater);
        s212.addProperty(hasHomogenizer, Homogenizer);
        s212.addProperty(hasShape, model.createResource(pre + "s212Shape"));

// --- s212: Heater=On, Homogenizer=Off, x>=55, p<=10

// ex:s212HeaterShape：
        Resource s212HeaterShape = model.createResource(pre + "s212HeaterShape");
        s212HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Heater = On
        Resource heaterShapePropMode_s212 = model.createResource();
        heaterShapePropMode_s212.addProperty(shPath, mode);
        heaterShapePropMode_s212.addLiteral(shMinCount, 1);
        heaterShapePropMode_s212.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s212.addProperty(shHasValue, onMode);

// x >= 55
        Resource heaterShapePropX_s212 = model.createResource();
        heaterShapePropX_s212.addProperty(shPath, x);
        heaterShapePropX_s212.addLiteral(shMinCount, 1);
        heaterShapePropX_s212.addLiteral(shMaxCount, 1);
        heaterShapePropX_s212.addLiteral(shMinInclusive, 55);

        s212HeaterShape.addProperty(shProperty, heaterShapePropMode_s212);
        s212HeaterShape.addProperty(shProperty, heaterShapePropX_s212);

// ex:s212HomogenizerShape：
        Resource s212HomogenizerShape = model.createResource(pre + "s212HomogenizerShape");
        s212HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Homogenizer = Off
        Resource homogenizerShapePropMode_s212 = model.createResource();
        homogenizerShapePropMode_s212.addProperty(shPath, mode);
        homogenizerShapePropMode_s212.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s212.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s212.addProperty(shHasValue, offMode);

// p <= 10
        Resource homogenizerShapePropP_s212 = model.createResource();
        homogenizerShapePropP_s212.addProperty(shPath, P);
        homogenizerShapePropP_s212.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s212.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s212.addLiteral(shMaxInclusive, 10);

        s212HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s212);
        s212HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s212);

// ex:s212Shape：
        Resource s212Shape = model.createResource(pre + "s212Shape");
        s212Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s212Shape.addProperty(shTargetClass, State);

// stateHeaterShapeProp_s212
        Resource stateHeaterShapeProp_s212 = model.createResource();
        stateHeaterShapeProp_s212.addProperty(shPath, hasHeater);
        stateHeaterShapeProp_s212.addLiteral(shMinCount, 1);
        stateHeaterShapeProp_s212.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp_s212.addProperty(shNode, s212HeaterShape);

// stateHomogenizerShapeProp_s212
        Resource stateHomogenizerShapeProp_s212 = model.createResource();
        stateHomogenizerShapeProp_s212.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp_s212.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp_s212.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp_s212.addProperty(shNode, s212HomogenizerShape);

        s212Shape.addProperty(shProperty, stateHeaterShapeProp_s212);
        s212Shape.addProperty(shProperty, stateHomogenizerShapeProp_s212);

        //=================================================
        // create state s222，
        // --- s222 Heater=Off, Homogenizer=Off, x>=55, p<=10
        //=================================================

        // ex:s222 a ex:State ;
//   ex:hasHeater ex:Heater ;
//   ex:hasHomogenizer ex:Homogenizer ;
//   ex:hasShape ex:s222Shape .
        Resource s222 = model.createResource(pre + "s222");
        s222.addProperty(RDF.type, State);
        s222.addProperty(hasHeater, Heater);
        s222.addProperty(hasHomogenizer, Homogenizer);
        s222.addProperty(hasShape, model.createResource(pre + "s222Shape"));

// --- s222: Heater=Off, Homogenizer=Off, x>=55, p<=10

// ex:s222HeaterShape：
        Resource s222HeaterShape = model.createResource(pre + "s222HeaterShape");
        s222HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Heater = Off
        Resource heaterShapePropMode_s222 = model.createResource();
        heaterShapePropMode_s222.addProperty(shPath, mode);
        heaterShapePropMode_s222.addLiteral(shMinCount, 1);
        heaterShapePropMode_s222.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s222.addProperty(shHasValue, offMode);

// x >= 55
        Resource heaterShapePropX_s222 = model.createResource();
        heaterShapePropX_s222.addProperty(shPath, x);
        heaterShapePropX_s222.addLiteral(shMinCount, 1);
        heaterShapePropX_s222.addLiteral(shMaxCount, 1);
        heaterShapePropX_s222.addLiteral(shMinInclusive, 55);

        s222HeaterShape.addProperty(shProperty, heaterShapePropMode_s222);
        s222HeaterShape.addProperty(shProperty, heaterShapePropX_s222);

// ex:s222HomogenizerShape：
        Resource s222HomogenizerShape = model.createResource(pre + "s222HomogenizerShape");
        s222HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Homogenizer = Off
        Resource homogenizerShapePropMode_s222 = model.createResource();
        homogenizerShapePropMode_s222.addProperty(shPath, mode);
        homogenizerShapePropMode_s222.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s222.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s222.addProperty(shHasValue, offMode);

// p <= 10
        Resource homogenizerShapePropP_s222 = model.createResource();
        homogenizerShapePropP_s222.addProperty(shPath, P);
        homogenizerShapePropP_s222.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s222.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s222.addLiteral(shMaxInclusive, 10);

        s222HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s222);
        s222HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s222);

// ex:s222Shape：
        Resource s222Shape = model.createResource(pre + "s222Shape");
        s222Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s222Shape.addProperty(shTargetClass, State);

// stateHeaterShapeProp_s222
        Resource stateHeaterShapeProp_s222 = model.createResource();
        stateHeaterShapeProp_s222.addProperty(shPath, hasHeater);
        stateHeaterShapeProp_s222.addLiteral(shMinCount, 1);
        stateHeaterShapeProp_s222.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp_s222.addProperty(shNode, s222HeaterShape);

// stateHomogenizerShapeProp_s222
        Resource stateHomogenizerShapeProp_s222 = model.createResource();
        stateHomogenizerShapeProp_s222.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp_s222.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp_s222.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp_s222.addProperty(shNode, s222HomogenizerShape);

        s222Shape.addProperty(shProperty, stateHeaterShapeProp_s222);
        s222Shape.addProperty(shProperty, stateHomogenizerShapeProp_s222);

        //=================================================
        // create state s322，
        //=================================================
        // --- s322 Heater=Off, Homogenizer=Off, 55=<x<=65, 10=<p<=20

        // ex:s322 a ex:State ;
//   ex:hasHeater ex:Heater ;
//   ex:hasHomogenizer ex:Homogenizer ;
//   ex:hasShape ex:s322Shape .
        Resource s322 = model.createResource(pre + "s322");
        s322.addProperty(RDF.type, State);
        s322.addProperty(hasHeater, Heater);
        s322.addProperty(hasHomogenizer, Homogenizer);
        s322.addProperty(hasShape, model.createResource(pre + "s322Shape"));
// --- s322: Heater=Off, Homogenizer=Off, x ∈ [55,65], p ∈ [10,20]

// ex:s322HeaterShape
        Resource s322HeaterShape = model.createResource(pre + "s322HeaterShape");
        s322HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Heater mode = Off
        Resource heaterShapePropMode_s322 = model.createResource();
        heaterShapePropMode_s322.addProperty(shPath, mode);
        heaterShapePropMode_s322.addLiteral(shMinCount, 1);
        heaterShapePropMode_s322.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s322.addProperty(shHasValue, offMode);

// Heater x ∈ [55, 65]
        Resource heaterShapePropX_s322 = model.createResource();
        heaterShapePropX_s322.addProperty(shPath, x);
        heaterShapePropX_s322.addLiteral(shMinCount, 1);
        heaterShapePropX_s322.addLiteral(shMaxCount, 1);
        heaterShapePropX_s322.addLiteral(shMinInclusive, 55);
        heaterShapePropX_s322.addLiteral(shMaxInclusive, 65);

        s322HeaterShape.addProperty(shProperty, heaterShapePropMode_s322);
        s322HeaterShape.addProperty(shProperty, heaterShapePropX_s322);

// ex:s322HomogenizerShape
        Resource s322HomogenizerShape = model.createResource(pre + "s322HomogenizerShape");
        s322HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Homogenizer mode = Off
        Resource homogenizerShapePropMode_s322 = model.createResource();
        homogenizerShapePropMode_s322.addProperty(shPath, mode);
        homogenizerShapePropMode_s322.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s322.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s322.addProperty(shHasValue, offMode);

// p ∈ [10, 20]
        Resource homogenizerShapePropP_s322 = model.createResource();
        homogenizerShapePropP_s322.addProperty(shPath, P);
        homogenizerShapePropP_s322.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s322.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s322.addLiteral(shMinInclusive, 10);
        homogenizerShapePropP_s322.addLiteral(shMaxInclusive, 20);

        s322HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s322);
        s322HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s322);

// ex:s322Shape
        Resource s322Shape = model.createResource(pre + "s322Shape");
        s322Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s322Shape.addProperty(shTargetClass, State);

// stateHeaterShapeProp_s322
        Resource stateHeaterShapeProp_s322 = model.createResource();
        stateHeaterShapeProp_s322.addProperty(shPath, hasHeater);
        stateHeaterShapeProp_s322.addLiteral(shMinCount, 1);
        stateHeaterShapeProp_s322.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp_s322.addProperty(shNode, s322HeaterShape);

// stateHomogenizerShapeProp_s322
        Resource stateHomogenizerShapeProp_s322 = model.createResource();
        stateHomogenizerShapeProp_s322.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp_s322.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp_s322.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp_s322.addProperty(shNode, s322HomogenizerShape);

        s322Shape.addProperty(shProperty, stateHeaterShapeProp_s322);
        s322Shape.addProperty(shProperty, stateHomogenizerShapeProp_s322);

        //=================================================
        // create state s311
        // Heater=On, Homogenizer=On
        // x>=55 & <=65, p>=10 & <=20
        //=================================================

        Resource s311 = model.createResource(pre + "s311");
        s311.addProperty(RDF.type, State);
        s311.addProperty(hasHeater, Heater);
        s311.addProperty(hasHomogenizer, Homogenizer);
        s311.addProperty(hasShape, model.createResource(pre + "s311Shape"));

// s311: Heater=On, Homogenizer=On, x ∈ [55, 65], p ∈ [10, 20]

        Resource s311HeaterShape = model.createResource(pre + "s311HeaterShape");
        s311HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Heater=On
        Resource heaterShapePropMode_s311 = model.createResource();
        heaterShapePropMode_s311.addProperty(shPath, mode);
        heaterShapePropMode_s311.addLiteral(shMinCount, 1);
        heaterShapePropMode_s311.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s311.addProperty(shHasValue, onMode);

// x ∈ [55, 65]
        Resource heaterShapePropX_s311 = model.createResource();
        heaterShapePropX_s311.addProperty(shPath, x);
        heaterShapePropX_s311.addLiteral(shMinCount, 1);
        heaterShapePropX_s311.addLiteral(shMaxCount, 1);
        heaterShapePropX_s311.addLiteral(shMinInclusive, 55);
        heaterShapePropX_s311.addLiteral(shMaxInclusive, 65);

        s311HeaterShape.addProperty(shProperty, heaterShapePropMode_s311);
        s311HeaterShape.addProperty(shProperty, heaterShapePropX_s311);

        Resource s311HomogenizerShape = model.createResource(pre + "s311HomogenizerShape");
        s311HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Homogenizer=On
        Resource homogenizerShapePropMode_s311 = model.createResource();
        homogenizerShapePropMode_s311.addProperty(shPath, mode);
        homogenizerShapePropMode_s311.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s311.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s311.addProperty(shHasValue, onMode);

// p ∈ [10, 20]
        Resource homogenizerShapePropP_s311 = model.createResource();
        homogenizerShapePropP_s311.addProperty(shPath, P);
        homogenizerShapePropP_s311.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s311.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s311.addLiteral(shMinInclusive, 10);
        homogenizerShapePropP_s311.addLiteral(shMaxInclusive, 20);

        s311HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s311);
        s311HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s311);

        Resource s311Shape = model.createResource(pre + "s311Shape");
        s311Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s311Shape.addProperty(shTargetClass, State);

        Resource stateHeaterShapeProp_s311 = model.createResource();
        stateHeaterShapeProp_s311.addProperty(shPath, hasHeater);
        stateHeaterShapeProp_s311.addLiteral(shMinCount, 1);
        stateHeaterShapeProp_s311.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp_s311.addProperty(shNode, s311HeaterShape);

        Resource stateHomogenizerShapeProp_s311 = model.createResource();
        stateHomogenizerShapeProp_s311.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp_s311.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp_s311.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp_s311.addProperty(shNode, s311HomogenizerShape);

        s311Shape.addProperty(shProperty, stateHeaterShapeProp_s311);
        s311Shape.addProperty(shProperty, stateHomogenizerShapeProp_s311);

        //=================================================
// create state s312
// Heater=On, Homogenizer=Off
// x>=55 & <=65, p>=10 & <=20
//=================================================

        Resource s312 = model.createResource(pre + "s312");
        s312.addProperty(RDF.type, State);
        s312.addProperty(hasHeater, Heater);
        s312.addProperty(hasHomogenizer, Homogenizer);
        s312.addProperty(hasShape, model.createResource(pre + "s312Shape"));

// s312: Heater=On, Homogenizer=Off, x ∈ [55, 65], p ∈ [10, 20]

        Resource s312HeaterShape = model.createResource(pre + "s312HeaterShape");
        s312HeaterShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Heater=On
        Resource heaterShapePropMode_s312 = model.createResource();
        heaterShapePropMode_s312.addProperty(shPath, mode);
        heaterShapePropMode_s312.addLiteral(shMinCount, 1);
        heaterShapePropMode_s312.addLiteral(shMaxCount, 1);
        heaterShapePropMode_s312.addProperty(shHasValue, onMode);

// x ∈ [55, 65]
        Resource heaterShapePropX_s312 = model.createResource();
        heaterShapePropX_s312.addProperty(shPath, x);
        heaterShapePropX_s312.addLiteral(shMinCount, 1);
        heaterShapePropX_s312.addLiteral(shMaxCount, 1);
        heaterShapePropX_s312.addLiteral(shMinInclusive, 55);
        heaterShapePropX_s312.addLiteral(shMaxInclusive, 65);

        s312HeaterShape.addProperty(shProperty, heaterShapePropMode_s312);
        s312HeaterShape.addProperty(shProperty, heaterShapePropX_s312);

        Resource s312HomogenizerShape = model.createResource(pre + "s312HomogenizerShape");
        s312HomogenizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Homogenizer=Off
        Resource homogenizerShapePropMode_s312 = model.createResource();
        homogenizerShapePropMode_s312.addProperty(shPath, mode);
        homogenizerShapePropMode_s312.addLiteral(shMinCount, 1);
        homogenizerShapePropMode_s312.addLiteral(shMaxCount, 1);
        homogenizerShapePropMode_s312.addProperty(shHasValue, offMode);

// p ∈ [10, 20]
        Resource homogenizerShapePropP_s312 = model.createResource();
        homogenizerShapePropP_s312.addProperty(shPath, P);
        homogenizerShapePropP_s312.addLiteral(shMinCount, 1);
        homogenizerShapePropP_s312.addLiteral(shMaxCount, 1);
        homogenizerShapePropP_s312.addLiteral(shMinInclusive, 10);
        homogenizerShapePropP_s312.addLiteral(shMaxInclusive, 20);

        s312HomogenizerShape.addProperty(shProperty, homogenizerShapePropMode_s312);
        s312HomogenizerShape.addProperty(shProperty, homogenizerShapePropP_s312);

        Resource s312Shape = model.createResource(pre + "s312Shape");
        s312Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s312Shape.addProperty(shTargetClass, State);

        Resource stateHeaterShapeProp_s312 = model.createResource();
        stateHeaterShapeProp_s312.addProperty(shPath, hasHeater);
        stateHeaterShapeProp_s312.addLiteral(shMinCount, 1);
        stateHeaterShapeProp_s312.addLiteral(shMaxCount, 1);
        stateHeaterShapeProp_s312.addProperty(shNode, s312HeaterShape);

        Resource stateHomogenizerShapeProp_s312 = model.createResource();
        stateHomogenizerShapeProp_s312.addProperty(shPath, hasHomogenizer);
        stateHomogenizerShapeProp_s312.addLiteral(shMinCount, 1);
        stateHomogenizerShapeProp_s312.addLiteral(shMaxCount, 1);
        stateHomogenizerShapeProp_s312.addProperty(shNode, s312HomogenizerShape);

        s312Shape.addProperty(shProperty, stateHeaterShapeProp_s312);
        s312Shape.addProperty(shProperty, stateHomogenizerShapeProp_s312);

        //------------------- s412 -----------------------

        //=================================================
// create state s412
// Pasteurization = On
// x>=65 & <=90, p<=10
//=================================================

        Resource s412 = model.createResource(pre + "s412");
        s412.addProperty(RDF.type, State);
//  hasPasteurizer Pasteurizer
        s412.addProperty(model.createProperty(pre, "hasPasteurizer"), Pasteurizer);
        s412.addProperty(hasShape, model.createResource(pre + "s412Shape"));

// s412: Pasteurization=On, x ∈ [65, 90], p ≤ 10

        Resource s412PasteurizerShape = model.createResource(pre + "s412PasteurizerShape");
        s412PasteurizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Pasteurizer = On
        Resource pasteurizerShapePropMode_s412 = model.createResource();
        pasteurizerShapePropMode_s412.addProperty(shPath, mode);
        pasteurizerShapePropMode_s412.addLiteral(shMinCount, 1);
        pasteurizerShapePropMode_s412.addLiteral(shMaxCount, 1);
        pasteurizerShapePropMode_s412.addProperty(shHasValue, onMode);

// x ∈ [65, 90]
        Resource pasteurizerShapePropX_s412 = model.createResource();
        pasteurizerShapePropX_s412.addProperty(shPath, x);
        pasteurizerShapePropX_s412.addLiteral(shMinCount, 1);
        pasteurizerShapePropX_s412.addLiteral(shMaxCount, 1);
        pasteurizerShapePropX_s412.addLiteral(shMinInclusive, 65);
        pasteurizerShapePropX_s412.addLiteral(shMaxInclusive, 90);

// p ≤ 10
        Resource pasteurizerShapePropP_s412 = model.createResource();
        pasteurizerShapePropP_s412.addProperty(shPath, P);
        pasteurizerShapePropP_s412.addLiteral(shMinCount, 1);
        pasteurizerShapePropP_s412.addLiteral(shMaxCount, 1);
        pasteurizerShapePropP_s412.addLiteral(shMaxInclusive, 10);

        s412PasteurizerShape.addProperty(shProperty, pasteurizerShapePropMode_s412);
        s412PasteurizerShape.addProperty(shProperty, pasteurizerShapePropX_s412);
        s412PasteurizerShape.addProperty(shProperty, pasteurizerShapePropP_s412);

        Resource s412Shape = model.createResource(pre + "s412Shape");
        s412Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s412Shape.addProperty(shTargetClass, State);

// statePasteurizerShapeProp_s412
        Resource statePasteurizerShapeProp_s412 = model.createResource();
        statePasteurizerShapeProp_s412.addProperty(shPath, model.createProperty(pre, "hasPasteurizer"));
        statePasteurizerShapeProp_s412.addLiteral(shMinCount, 1);
        statePasteurizerShapeProp_s412.addLiteral(shMaxCount, 1);
        statePasteurizerShapeProp_s412.addProperty(shNode, s412PasteurizerShape);

        s412Shape.addProperty(shProperty, statePasteurizerShapeProp_s412);

        //=================================================
// create state s422
// Pasteurization = Off
// x>=65 & <=90, p<=10
//=================================================

        Resource s422 = model.createResource(pre + "s422");
        s422.addProperty(RDF.type, State);
        s422.addProperty(model.createProperty(pre, "hasPasteurizer"), Pasteurizer);
        s422.addProperty(hasShape, model.createResource(pre + "s422Shape"));

// s422: Pasteurization=Off, x ∈ [65, 90], p ≤ 10

        Resource s422PasteurizerShape = model.createResource(pre + "s422PasteurizerShape");
        s422PasteurizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Pasteurizer = Off
        Resource pasteurizerShapePropMode_s422 = model.createResource();
        pasteurizerShapePropMode_s422.addProperty(shPath, mode);
        pasteurizerShapePropMode_s422.addLiteral(shMinCount, 1);
        pasteurizerShapePropMode_s422.addLiteral(shMaxCount, 1);
        pasteurizerShapePropMode_s422.addProperty(shHasValue, offMode);

// x ∈ [65, 90]
        Resource pasteurizerShapePropX_s422 = model.createResource();
        pasteurizerShapePropX_s422.addProperty(shPath, x);
        pasteurizerShapePropX_s422.addLiteral(shMinCount, 1);
        pasteurizerShapePropX_s422.addLiteral(shMaxCount, 1);
        pasteurizerShapePropX_s422.addLiteral(shMinInclusive, 65);
        pasteurizerShapePropX_s422.addLiteral(shMaxInclusive, 90);

// p ≤ 10
        Resource pasteurizerShapePropP_s422 = model.createResource();
        pasteurizerShapePropP_s422.addProperty(shPath, P);
        pasteurizerShapePropP_s422.addLiteral(shMinCount, 1);
        pasteurizerShapePropP_s422.addLiteral(shMaxCount, 1);
        pasteurizerShapePropP_s422.addLiteral(shMaxInclusive, 10);

        s422PasteurizerShape.addProperty(shProperty, pasteurizerShapePropMode_s422);
        s422PasteurizerShape.addProperty(shProperty, pasteurizerShapePropX_s422);
        s422PasteurizerShape.addProperty(shProperty, pasteurizerShapePropP_s422);

        Resource s422Shape = model.createResource(pre + "s422Shape");
        s422Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s422Shape.addProperty(shTargetClass, State);

        Resource statePasteurizerShapeProp_s422 = model.createResource();
        statePasteurizerShapeProp_s422.addProperty(shPath, model.createProperty(pre, "hasPasteurizer"));
        statePasteurizerShapeProp_s422.addLiteral(shMinCount, 1);
        statePasteurizerShapeProp_s422.addLiteral(shMaxCount, 1);
        statePasteurizerShapeProp_s422.addProperty(shNode, s422PasteurizerShape);

        s422Shape.addProperty(shProperty, statePasteurizerShapeProp_s422);

        //=================================================
// create state s512
// Pasteurization = On
// x>=90 & <=95, p<=10
//=================================================
        //------------------- s512 -----------------------

        Resource s512 = model.createResource(pre + "s512");
        s512.addProperty(RDF.type, State);
        s512.addProperty(model.createProperty(pre, "hasPasteurizer"), Pasteurizer);
        s512.addProperty(hasShape, model.createResource(pre + "s512Shape"));

// s512: Pasteurization=On, x ∈ [90, 95], p ≤ 10

        Resource s512PasteurizerShape = model.createResource(pre + "s512PasteurizerShape");
        s512PasteurizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Pasteurizer = On
        Resource pasteurizerShapePropMode_s512 = model.createResource();
        pasteurizerShapePropMode_s512.addProperty(shPath, mode);
        pasteurizerShapePropMode_s512.addLiteral(shMinCount, 1);
        pasteurizerShapePropMode_s512.addLiteral(shMaxCount, 1);
        pasteurizerShapePropMode_s512.addProperty(shHasValue, onMode);

// x ∈ [90, 95]
        Resource pasteurizerShapePropX_s512 = model.createResource();
        pasteurizerShapePropX_s512.addProperty(shPath, x);
        pasteurizerShapePropX_s512.addLiteral(shMinCount, 1);
        pasteurizerShapePropX_s512.addLiteral(shMaxCount, 1);
        pasteurizerShapePropX_s512.addLiteral(shMinInclusive, 90);
        pasteurizerShapePropX_s512.addLiteral(shMaxInclusive, 95);

// p ≤ 10
        Resource pasteurizerShapePropP_s512 = model.createResource();
        pasteurizerShapePropP_s512.addProperty(shPath, P);
        pasteurizerShapePropP_s512.addLiteral(shMinCount, 1);
        pasteurizerShapePropP_s512.addLiteral(shMaxCount, 1);
        pasteurizerShapePropP_s512.addLiteral(shMaxInclusive, 10);

        s512PasteurizerShape.addProperty(shProperty, pasteurizerShapePropMode_s512);
        s512PasteurizerShape.addProperty(shProperty, pasteurizerShapePropX_s512);
        s512PasteurizerShape.addProperty(shProperty, pasteurizerShapePropP_s512);

        Resource s512Shape = model.createResource(pre + "s512Shape");
        s512Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s512Shape.addProperty(shTargetClass, State);

        Resource statePasteurizerShapeProp_s512 = model.createResource();
        statePasteurizerShapeProp_s512.addProperty(shPath, model.createProperty(pre, "hasPasteurizer"));
        statePasteurizerShapeProp_s512.addLiteral(shMinCount, 1);
        statePasteurizerShapeProp_s512.addLiteral(shMaxCount, 1);
        statePasteurizerShapeProp_s512.addProperty(shNode, s512PasteurizerShape);

        s512Shape.addProperty(shProperty, statePasteurizerShapeProp_s512);

        //=================================================
// create state s522
// Pasteurization = Off
// x>=90 & <=95, p<=10
//=================================================

//        //------------------- s522 -----------------------

        Resource s522 = model.createResource(pre + "s522");
        s522.addProperty(RDF.type, State);
        s522.addProperty(model.createProperty(pre, "hasPasteurizer"), Pasteurizer);
        s522.addProperty(hasShape, model.createResource(pre + "s522Shape"));

// s522: Pasteurization=Off, x ∈ [90, 95], p ≤ 10

        Resource s522PasteurizerShape = model.createResource(pre + "s522PasteurizerShape");
        s522PasteurizerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Pasteurizer = Off
        Resource pasteurizerShapePropMode_s522 = model.createResource();
        pasteurizerShapePropMode_s522.addProperty(shPath, mode);
        pasteurizerShapePropMode_s522.addLiteral(shMinCount, 1);
        pasteurizerShapePropMode_s522.addLiteral(shMaxCount, 1);
        pasteurizerShapePropMode_s522.addProperty(shHasValue, offMode);

// x ∈ [90, 95]
        Resource pasteurizerShapePropX_s522 = model.createResource();
        pasteurizerShapePropX_s522.addProperty(shPath, x);
        pasteurizerShapePropX_s522.addLiteral(shMinCount, 1);
        pasteurizerShapePropX_s522.addLiteral(shMaxCount, 1);
        pasteurizerShapePropX_s522.addLiteral(shMinInclusive, 90);
        pasteurizerShapePropX_s522.addLiteral(shMaxInclusive, 95);

// p ≤ 10
        Resource pasteurizerShapePropP_s522 = model.createResource();
        pasteurizerShapePropP_s522.addProperty(shPath, P);
        pasteurizerShapePropP_s522.addLiteral(shMinCount, 1);
        pasteurizerShapePropP_s522.addLiteral(shMaxCount, 1);
        pasteurizerShapePropP_s522.addLiteral(shMaxInclusive, 10);

        s522PasteurizerShape.addProperty(shProperty, pasteurizerShapePropMode_s522);
        s522PasteurizerShape.addProperty(shProperty, pasteurizerShapePropX_s522);
        s522PasteurizerShape.addProperty(shProperty, pasteurizerShapePropP_s522);

        Resource s522Shape = model.createResource(pre + "s522Shape");
        s522Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s522Shape.addProperty(shTargetClass, State);

        Resource statePasteurizerShapeProp_s522 = model.createResource();
        statePasteurizerShapeProp_s522.addProperty(shPath, model.createProperty(pre, "hasPasteurizer"));
        statePasteurizerShapeProp_s522.addLiteral(shMinCount, 1);
        statePasteurizerShapeProp_s522.addLiteral(shMaxCount, 1);
        statePasteurizerShapeProp_s522.addProperty(shNode, s522PasteurizerShape);

        s522Shape.addProperty(shProperty, statePasteurizerShapeProp_s522);

        //=================================================
// create state s612
// Cooler = On
// x>=40 & <=45, p<=10
//=================================================

//        //------------------- s612 -----------------------
        Resource s612 = model.createResource(pre + "s612");
        s612.addProperty(RDF.type, State);
        s612.addProperty(model.createProperty(pre, "hasCooler"), Cooler);
        s612.addProperty(hasShape, model.createResource(pre + "s612Shape"));

// s612: Cooler=On, x ∈ [40, 45], p ≤ 10

        Resource s612CoolerShape = model.createResource(pre + "s612CoolerShape");
        s612CoolerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// Cooler=On
        Resource coolerShapePropMode_s612 = model.createResource();
        coolerShapePropMode_s612.addProperty(shPath, mode);
        coolerShapePropMode_s612.addLiteral(shMinCount, 1);
        coolerShapePropMode_s612.addLiteral(shMaxCount, 1);
        coolerShapePropMode_s612.addProperty(shHasValue, onMode);

// x ∈ [40, 45]
        Resource coolerShapePropX_s612 = model.createResource();
        coolerShapePropX_s612.addProperty(shPath, x);
        coolerShapePropX_s612.addLiteral(shMinCount, 1);
        coolerShapePropX_s612.addLiteral(shMaxCount, 1);
        coolerShapePropX_s612.addLiteral(shMinInclusive, 40);
        coolerShapePropX_s612.addLiteral(shMaxInclusive, 45);

// p ≤ 10
        Resource coolerShapePropP_s612 = model.createResource();
        coolerShapePropP_s612.addProperty(shPath, P);
        coolerShapePropP_s612.addLiteral(shMinCount, 1);
        coolerShapePropP_s612.addLiteral(shMaxCount, 1);
        coolerShapePropP_s612.addLiteral(shMaxInclusive, 10);

        s612CoolerShape.addProperty(shProperty, coolerShapePropMode_s612);
        s612CoolerShape.addProperty(shProperty, coolerShapePropX_s612);
        s612CoolerShape.addProperty(shProperty, coolerShapePropP_s612);

        Resource s612Shape = model.createResource(pre + "s612Shape");
        s612Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s612Shape.addProperty(shTargetClass, State);

        Resource stateCoolerShapeProp_s612 = model.createResource();
        stateCoolerShapeProp_s612.addProperty(shPath, model.createProperty(pre, "hasCooler"));
        stateCoolerShapeProp_s612.addLiteral(shMinCount, 1);
        stateCoolerShapeProp_s612.addLiteral(shMaxCount, 1);
        stateCoolerShapeProp_s612.addProperty(shNode, s612CoolerShape);

        s612Shape.addProperty(shProperty, stateCoolerShapeProp_s612);

        //=================================================
// create state s622
// Cooler = Off
// x>=40 & <=45, p<=10
//=================================================

//        //------------------- s622 -----------------------

        Resource s622 = model.createResource(pre + "s622");
        s622.addProperty(RDF.type, State);

        s622.addProperty(model.createProperty(pre, "hasCooler"), Cooler);
        s622.addProperty(hasShape, model.createResource(pre + "s622Shape"));

// s622: Cooler=Off, x ∈ [40, 45], p ≤ 10

        Resource s622CoolerShape = model.createResource(pre + "s622CoolerShape");
        s622CoolerShape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));

// 1) Cooler = Off
        Resource coolerShapePropMode_s622 = model.createResource();
        coolerShapePropMode_s622.addProperty(shPath, mode);
        coolerShapePropMode_s622.addLiteral(shMinCount, 1);
        coolerShapePropMode_s622.addLiteral(shMaxCount, 1);
        coolerShapePropMode_s622.addProperty(shHasValue, offMode);

// 2) x ∈ [40, 45]
        Resource coolerShapePropX_s622 = model.createResource();
        coolerShapePropX_s622.addProperty(shPath, x);
        coolerShapePropX_s622.addLiteral(shMinCount, 1);
        coolerShapePropX_s622.addLiteral(shMaxCount, 1);
        coolerShapePropX_s622.addLiteral(shMinInclusive, 40);
        coolerShapePropX_s622.addLiteral(shMaxInclusive, 45);

// 3) p ≤ 10
        Resource coolerShapePropP_s622 = model.createResource();
        coolerShapePropP_s622.addProperty(shPath, P);
        coolerShapePropP_s622.addLiteral(shMinCount, 1);
        coolerShapePropP_s622.addLiteral(shMaxCount, 1);
        coolerShapePropP_s622.addLiteral(shMaxInclusive, 10);

        s622CoolerShape.addProperty(shProperty, coolerShapePropMode_s622);
        s622CoolerShape.addProperty(shProperty, coolerShapePropX_s622);
        s622CoolerShape.addProperty(shProperty, coolerShapePropP_s622);

// s622Shape， SHACL
        Resource s622Shape = model.createResource(pre + "s622Shape");
        s622Shape.addProperty(RDF.type, model.createResource(shNS + "NodeShape"));
        s622Shape.addProperty(shTargetClass, State);

        Resource stateCoolerShapeProp_s622 = model.createResource();
        stateCoolerShapeProp_s622.addProperty(shPath, model.createProperty(pre, "hasCooler"));
        stateCoolerShapeProp_s622.addLiteral(shMinCount, 1);
        stateCoolerShapeProp_s622.addLiteral(shMaxCount, 1);
        stateCoolerShapeProp_s622.addProperty(shNode, s622CoolerShape);

        s622Shape.addProperty(shProperty, stateCoolerShapeProp_s622);

        // NEW - Set s622 as the goal state
        yogurtProduction.addProperty(goalState, s622);

        IsNextBuiltin isNextBuiltin = new IsNextBuiltin(dataset);
        BuiltinRegistry.theRegistry.register(isNextBuiltin);

        ModeChange modeChange = new ModeChange(dataset);
        BuiltinRegistry.theRegistry.register(modeChange);

        ModeChange2 modeChange2 = new ModeChange2(dataset);
        BuiltinRegistry.theRegistry.register(modeChange2);

        ModeChange3 modeChange3 = new ModeChange3(dataset);
        BuiltinRegistry.theRegistry.register(modeChange3);

        ModeChange4 modeChange4 = new ModeChange4(dataset);
        BuiltinRegistry.theRegistry.register(modeChange4);

        // [rule1: (?a ex:next ?b) <- isNext(?a, ?b), ?a a ex:state, ?b a ex:state ]
        String rules1 = "[rule1: (?a " + nextpredicate + " ?b) <- "
                + "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) "
                + "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) "
                + " isNext(?a, ?b)" +
                "]";

        String rules2 =   "[rule2: (?a " + ModeChange +  " ?b) <- "
                + "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) "
                + "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) "
                + "mode_change(?a, ?b)"
                + "]";

        String rules3 = "[rule3: (?a " + ModeChange2 +  " ?b) <- " +
                "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) " +
                "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) " +
                "mode_change2(?a, ?b)" +
                "]";

        String rules4 = "[rule4: (?a " + ModeChange3 +  " ?b) <- " +
                "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) " +
                "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) " +
                "mode_change3(?a, ?b)" +
                "]";

        String rules5 = "[rule5: (?a " + ModeChange4 +  " ?b) <- " +
                "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) " +
                "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://anonymous.example.org#State>) " +
                "mode_change4(?a, ?b)" +
                "]";

        String combinedRules = rules1+ rules2 + rules3 + rules4+ rules5 ;

        List<Rule> allRules = Rule.parseRules(combinedRules);

        Reasoner reasoner = new GenericRuleReasoner(allRules);
        reasoner.setDerivationLogging(true);

        InfModel inf = ModelFactory.createInfModel(reasoner, model);

        StmtIterator iterBefore = model.listStatements();
        while (iterBefore.hasNext()) {
            Statement st = iterBefore.nextStatement();
            Resource  subject   = st.getSubject();     // get the subject
            Property  predicate = st.getPredicate();   // get the predicate
            RDFNode   object    = st.getObject();

            System.out.println(subject + " " + predicate + " " + object);
        }

        Path outputDir = Paths.get("target", "ttl");
        Files.createDirectories(outputDir);

        Path originalOutputPath = outputDir.resolve("knowledgeGraphWithSHACL_original2.ttl");
        try (FileOutputStream out = new FileOutputStream(originalOutputPath.toFile())) {
            RDFDataMgr.write(out, inf, RDFFormat.TRIG_PRETTY);
            System.out.println("The knowledge graph with original relationships has been written to: " + originalOutputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Process the inference model to create detailed relationships
        Path processedOutputPath = outputDir.resolve("knowledgeGraphWithSHACL_processed2.ttl");
        RDFPostProcessor.processInferenceModel(inf, processedOutputPath.toString());

        System.out.println("Processing complete. Both original and processed knowledge graphs have been saved.");
    }
}
