package org.example;

import static org.junit.Assert.*;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.BuiltinRegistry;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Test;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class IsNextBuiltinTest {
    @Test
    public void main() {
        // 1) Create dataset and default model
        Dataset dataset = DatasetFactory.create();
        Model model = dataset.getDefaultModel();

        String nextpredicate = "<http://example.org/states#next>";
        String ModeChange = "<http://example.org/states#ModeChange>";

        // 2) Namespace prefixes
        String PRE = "http://anonymous.example.org#";
        String SH  = "http://www.w3.org/ns/shacl#";
        model.setNsPrefix("pre", PRE);
        model.setNsPrefix("sh", SH);
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("xsd", XSD.getURI());

        // 3) Define resources and properties
        Resource State      = model.createResource(PRE + "State");
        Resource ModeRecord = model.createResource(PRE + "ModeRecord");
        Resource ODE        = model.createResource(PRE + "ODE");
        Resource OnMode     = model.createResource(PRE + "OnMode");
        Resource OffMode    = model.createResource(PRE + "OffMode");

        Property hasDevice    = model.createProperty(PRE, "hasDevice");
        Property hasMode      = model.createProperty(PRE, "hasMode");
        Property hasODE       = model.createProperty(PRE, "hasODE");
        Property hasOven      = model.createProperty(PRE, "hasOven");
        Property hasShape     = model.createProperty(PRE, "hasShape");
        Property derivative   = model.createProperty(PRE, "derivative");
        Property evolvingVar  = model.createProperty(PRE, "evolvingVariable");
        Property startCond    = model.createProperty(PRE, "startingCondition");
        Property endCond      = model.createProperty(PRE, "endingCondition");
        Property domainConst  = model.createProperty(PRE, "evolutionDomainConstraint");
        Property xProp        = model.createProperty(PRE, "x");

        // SHACL properties
        Property shPath        = model.createProperty(SH, "path");
        Property shNode        = model.createProperty(SH, "node");
        Property shMinCount    = model.createProperty(SH, "minCount");
        Property shMaxCount    = model.createProperty(SH, "maxCount");
        Property shHasValue    = model.createProperty(SH, "hasValue");
        Property shMinInclusive= model.createProperty(SH, "minInclusive");
        Property shMaxInclusive= model.createProperty(SH, "maxInclusive");
        Property shProperty    = model.createProperty(SH, "property");
        Property shTargetClass = model.createProperty(SH, "targetClass");

        // 4) Oven device metadata
        Resource Oven = model.createResource(PRE + "Oven");
        Oven.addProperty(RDF.type, model.createResource("http://schema.org/Product"));
        Oven.addLiteral(model.createProperty("http://schema.org/capacity"), "50L");
        Oven.addLiteral(model.createProperty("http://schema.org/manufacturer"), "KitchenTech Ltd.");
        Oven.addLiteral(model.createProperty("http://schema.org/model"), "KT-2025");

        // 5) **NEW SAFE DDL DESIGN** - ModeRecords and ODEs
        Resource OvenOnMode  = model.createResource(PRE + "OvenOnMode");
        Resource OvenOffMode = model.createResource(PRE + "OvenOffMode");
        OvenOnMode.addProperty(RDF.type, ModeRecord)
                .addProperty(hasDevice, Oven)
                .addProperty(hasMode, OnMode);
        OvenOffMode.addProperty(RDF.type, ModeRecord)
                .addProperty(hasDevice, Oven)
                .addProperty(hasMode, OffMode);

        // OnMode: Can only start at low temp, must end at high temp, prevents overheat
        Resource odeOn  = model.createResource()
                .addProperty(RDF.type, ODE)
                .addLiteral(derivative, "(2620 - 10*x) / 4000")
                .addLiteral(startCond, "x <= 180")              // Can only start at <=180°
                .addLiteral(endCond,   "x >= 180")              // Must end when reaching 180°
                .addLiteral(domainConst, "x <= 200")            // Absolute safety limit
                .addProperty(evolvingVar, xProp);

        // OffMode: Forced start at high temp, cool down to low temp
        Resource odeOff = model.createResource()
                .addProperty(RDF.type, ODE)
                .addLiteral(derivative, "(-10*x + 200) / 4000")
                .addLiteral(startCond, "x >= 180")              // Forced start at >=180°
                .addLiteral(endCond,   "x <= 180")              // Cool down to 180°
                .addLiteral(domainConst, "x <= 200")             // absolute safety limit
                .addProperty(evolvingVar, xProp);

        OvenOnMode.addProperty(hasODE, odeOn);
        OvenOffMode.addProperty(hasODE, odeOff);

        // 6) **NEW STATE DESIGN** - Supporting correct connections for ModeChange and IsNext
        Resource s11 = model.createResource(PRE + "s11");
        Resource s12 = model.createResource(PRE + "s12");
        Resource s21 = model.createResource(PRE + "s21");
        Resource s22 = model.createResource(PRE + "s22");
        for (Resource s : new Resource[]{s11, s12, s21, s22}) {
            s.addProperty(RDF.type, State)
                    .addProperty(hasOven, Oven);
        }

        // s11: OffMode, x <= 180 (Low temperature off state)
        Resource s11Shape = model.createResource(PRE + "s11Shape");
        Resource s11OvenShape = model.createResource(PRE + "s11OvenShape");
        s11.addProperty(hasShape, s11Shape);
        s11Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasOven)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s11OvenShape));
        s11OvenShape.addProperty(RDF.type, model.createResource(SH + "NodeShape"));
        s11OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, model.createProperty(PRE, "mode"))
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addProperty(shHasValue, OffMode));
        s11OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, xProp)
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addLiteral(shMaxInclusive, 180));                // Low temperature upper limit


        // s12: OnMode, x <= 180 (Low temperature heating state) - Same constraints as s11, supports ModeChange
        Resource s12Shape = model.createResource(PRE + "s12Shape");
        Resource s12OvenShape = model.createResource(PRE + "s12OvenShape");
        s12.addProperty(hasShape, s12Shape);
        s12Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasOven)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s12OvenShape));
        s12OvenShape.addProperty(RDF.type, model.createResource(SH + "NodeShape"));
        s12OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, model.createProperty(PRE, "mode"))
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addProperty(shHasValue, OnMode));
        s12OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, xProp)
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addLiteral(shMaxInclusive, 180));                // Same low temperature constraints


        // s21: OnMode, x >= 180 (High temperature protection state)
        Resource s21Shape = model.createResource(PRE + "s21Shape");
        Resource s21OvenShape = model.createResource(PRE + "s21OvenShape");
        s21.addProperty(hasShape, s21Shape);
        s21Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasOven)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s21OvenShape));
        s21OvenShape.addProperty(RDF.type, model.createResource(SH + "NodeShape"));
        s21OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, model.createProperty(PRE, "mode"))
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addProperty(shHasValue, OnMode));
        s21OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, xProp)
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addLiteral(shMinInclusive, 180)
                .addLiteral(shMaxInclusive, 200));                // High temperature lower limit
                             // Safety upper limit

        // s22: OffMode, x >= 180 (High temperature heating state) - Same constraints as s21.
        Resource s22Shape = model.createResource(PRE + "s22Shape");
        Resource s22OvenShape = model.createResource(PRE + "s22OvenShape");
        s22.addProperty(hasShape, s22Shape);
        s22Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasOven)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s22OvenShape));
        s22OvenShape.addProperty(RDF.type, model.createResource(SH + "NodeShape"));
        s22OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, model.createProperty(PRE, "mode"))
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addProperty(shHasValue, OffMode));
        s22OvenShape.addProperty(shProperty, model.createResource()
                .addProperty(shPath, xProp)
                .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                .addLiteral(shMinInclusive, 180)
                .addLiteral(shMaxInclusive, 200));                // Same high temperature constraints


        // 7) Register builtins
        IsNextBuiltin isNextBuiltin = new IsNextBuiltin(dataset);
        BuiltinRegistry.theRegistry.register(isNextBuiltin);

        ModeChange modeChange = new ModeChange(dataset);
        BuiltinRegistry.theRegistry.register(modeChange);

        // 8) Rules for reasoning
        String rules1 = "[rule1: (?a " + nextpredicate + " ?b) <- "
                + "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                + "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                + " isNext(?a, ?b)" +
                "]";

        String rules2 =   "[rule2: (?a " + ModeChange +  " ?b) <- "
                + "(?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                + "(?b <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://anonymous.example.org#State>) "
                + "mode_change(?a, ?b)"
                + "]";

        List<Rule> allRules = Rule.parseRules(rules1+rules2);

        Reasoner reasoner = new GenericRuleReasoner(allRules);
        reasoner.setDerivationLogging(true);

        InfModel inf = ModelFactory.createInfModel(reasoner, model);

        StmtIterator iterBefore = model.listStatements();
        while (iterBefore.hasNext()) {
            Statement st = iterBefore.nextStatement();
            Resource  subject   = st.getSubject();
            Property  predicate = st.getPredicate();
            RDFNode   object    = st.getObject();

            System.out.println(subject + " " + predicate + " " + object);
        }

        // 9) Write TTL output
        try (FileOutputStream out = new FileOutputStream("D:\\TTL2\\knowledgeGraphWithSHACL_oven_safe.ttl")) {
            RDFDataMgr.write(out, inf, RDFFormat.TURTLE_PRETTY);
            System.out.println("Safe DDL knowledge graph has been successfully written to: D:\\TTL2\\knowledgeGraphWithSHACL_oven_safe.ttl");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Process the inference model to create detailed relationships
        String processedOutputPath = "D:\\TTL2\\knowledgeGraphWithSHACL_oven_safe_processed.ttl";
        RDFPostProcessor.processInferenceModel(inf, processedOutputPath);

        System.out.println("Processing complete. Both original and processed knowledge graphs have been saved.");

    }

}