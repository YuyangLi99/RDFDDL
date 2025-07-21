package org.example;

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

public class TankSystemTest {
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
        String SCHEMA = "http://schema.org/";
        model.setNsPrefix("pre", PRE);
        model.setNsPrefix("sh", SH);
        model.setNsPrefix("rdf", RDF.getURI());
        model.setNsPrefix("xsd", XSD.getURI());
        model.setNsPrefix("schema", SCHEMA);

        // 3) Define resources and properties
        Resource State      = model.createResource(PRE + "State");
        Resource ModeRecord = model.createResource(PRE + "ModeRecord");
        Resource ODE        = model.createResource(PRE + "ODE");
        Resource OnMode     = model.createResource(PRE + "OnMode");
        Resource OffMode    = model.createResource(PRE + "OffMode");

        Property hasDevice    = model.createProperty(PRE, "hasDevice");
        Property hasMode      = model.createProperty(PRE, "hasMode");
        Property hasODE       = model.createProperty(PRE, "hasODE");
        Property hasTank1     = model.createProperty(PRE, "hasTank1");
        Property hasTank2     = model.createProperty(PRE, "hasTank2");
        Property hasShape     = model.createProperty(PRE, "hasShape");
        Property derivative   = model.createProperty(PRE, "derivative");
        Property evolvingVar  = model.createProperty(PRE, "evolvingVariable");
        Property startCond    = model.createProperty(PRE, "startingCondition");
        Property endCond      = model.createProperty(PRE, "endingCondition");
        Property domainConst  = model.createProperty(PRE, "evolutionDomainConstraint");
        Property h1Prop       = model.createProperty(PRE, "h1");
        Property h2Prop       = model.createProperty(PRE, "h2");
        Property tProp        = model.createProperty(PRE + "t");
        Property mode         = model.createProperty(PRE, "mode");

        // SHACL properties
        Property shPath         = model.createProperty(SH, "path");
        Property shNode         = model.createProperty(SH, "node");
        Property shMinCount     = model.createProperty(SH, "minCount");
        Property shMaxCount     = model.createProperty(SH, "maxCount");
        Property shHasValue     = model.createProperty(SH, "hasValue");
        Property shMinInclusive = model.createProperty(SH, "minInclusive");
        Property shMaxInclusive = model.createProperty(SH, "maxInclusive");
        Property shMinExclusive = model.createProperty(SH, "minExclusive");
        Property shMaxExclusive = model.createProperty(SH, "maxExclusive");
        Property shProperty     = model.createProperty(SH, "property");
        Property shTargetClass  = model.createProperty(SH, "targetClass");

        // 4) Tank devices metadata
        Resource Tank1 = model.createResource(PRE + "Tank1");
        Tank1.addProperty(RDF.type, model.createResource(SCHEMA + "Product"));
        Tank1.addLiteral(model.createProperty(SCHEMA + "capacity"), "1200 L");
        Tank1.addLiteral(model.createProperty(SCHEMA + "manufacturer"), "GEA Group");
        Tank1.addLiteral(model.createProperty(SCHEMA + "model"), "ProTank VT‑1000");

        Resource Tank2 = model.createResource(PRE + "Tank2");
        Tank2.addProperty(RDF.type, model.createResource(SCHEMA + "Product"));
        Tank2.addLiteral(model.createProperty(SCHEMA + "capacity"), "1200 L");
        Tank2.addLiteral(model.createProperty(SCHEMA + "manufacturer"), "Tetra Pak Processing Systems");
        Tank2.addLiteral(model.createProperty(SCHEMA + "model"), "AsepticTank AT‑1500");

        // 5) ModeRecords and ODEs
        Resource FlowingOnMode = model.createResource(PRE + "FlowingOnMode");
        Resource FlowingOffMode = model.createResource(PRE + "FlowingOffMode");

        // FlowingOnMode
        FlowingOnMode.addProperty(RDF.type, ModeRecord)
                .addProperty(hasDevice, Tank1)
                .addProperty(hasDevice, Tank2)
                .addProperty(hasMode, OnMode);

        Resource odeOn = model.createResource()
                .addProperty(RDF.type, ODE)
                .addLiteral(derivative, "h1' = -0.0417*(h1 - h2 + 1)^(1/2), h2' = 0.0417*(h1 - h2 + 1)^(1/2)")
                .addLiteral(startCond, "h1=1 & h2=0")
                .addLiteral(endCond, "h1=0 & h2=1")
                .addLiteral(domainConst, "h1 >=0 & h1<= 1.0 & h2 >=0 & h2<= 1.0 ")
                .addProperty(evolvingVar, h1Prop)
                .addProperty(evolvingVar, h2Prop);
        FlowingOnMode.addProperty(hasODE, odeOn);

        // FlowingOffMode
       // Resource PipeValve = model.createResource(PRE + "PipeValve");
        FlowingOffMode.addProperty(RDF.type, ModeRecord)
                .addProperty(hasDevice, Tank1)
                .addProperty(hasDevice, Tank2)
                .addProperty(hasMode, OffMode);

        Resource odeOff = model.createResource()
                .addProperty(RDF.type, ODE)
                .addLiteral(derivative, "h1' = 0, h2' = 0")
                .addLiteral(startCond, "h1=0 & h2=1")
                .addLiteral(endCond, "false")
                .addLiteral(domainConst, " h1>=0 & h1<=1.0 & h2>=0 & h2<=1.0 ")
                .addProperty(evolvingVar, h1Prop)
                .addProperty(evolvingVar, h2Prop);
        FlowingOffMode.addProperty(hasODE, odeOff);

        // States and their shapes
        // Create states s0 through s4
        Resource[] states = new Resource[5];
        for (int i = 0; i <= 4; i++) {
            states[i] = model.createResource(PRE + "s" + i);
            states[i].addProperty(RDF.type, State)
                    .addProperty(hasTank1, Tank1)
                    .addProperty(hasTank2, Tank2);
        }

        // s0: Tank1 OffMode h1=1, Tank2 OffMode h2=0
        Resource s0Shape = model.createResource(PRE + "s0Shape");
        Resource s0Tank1Shape = model.createResource(PRE + "s0Tank1Shape");
        Resource s0Tank2Shape = model.createResource(PRE + "s0Tank2Shape");

        states[0].addProperty(hasShape, s0Shape);

        s0Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank1)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s0Tank1Shape))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank2)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s0Tank2Shape));

        s0Tank1Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h1Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(1.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OffMode));

        s0Tank2Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h2Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OffMode));

        // s1: Tank1 OnMode h1=1, Tank2 OnMode h2=0
        Resource s1Shape = model.createResource(PRE + "s1Shape");
        Resource s1Tank1Shape = model.createResource(PRE + "s1Tank1Shape");
        Resource s1Tank2Shape = model.createResource(PRE + "s1Tank2Shape");

        states[1].addProperty(hasShape, s1Shape);

        s1Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank1)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s1Tank1Shape))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank2)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s1Tank2Shape));

        s1Tank1Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h1Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(1.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OnMode));

        s1Tank2Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h2Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OnMode));

        // s2: Tank1 OnMode 0<h1<1, Tank2 OnMode 0.1<h2<1
        Resource s2Shape = model.createResource(PRE + "s2Shape");
        Resource s2Tank1Shape = model.createResource(PRE + "s2Tank1Shape");
        Resource s2Tank2Shape = model.createResource(PRE + "s2Tank2Shape");

        states[2].addProperty(hasShape, s2Shape);

        s2Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank1)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s2Tank1Shape))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank2)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s2Tank2Shape));

        s2Tank1Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h1Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addLiteral(shMinExclusive, model.createTypedLiteral(0.0))
                        .addLiteral(shMaxExclusive, model.createTypedLiteral(1.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OnMode));

        s2Tank2Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h2Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addLiteral(shMinExclusive, model.createTypedLiteral(0.0))
                        .addLiteral(shMaxExclusive, model.createTypedLiteral(1.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OnMode));

        // s3: Tank1 OnMode h1=0, Tank2 OnMode h2=1
        Resource s3Shape = model.createResource(PRE + "s3Shape");
        Resource s3Tank1Shape = model.createResource(PRE + "s3Tank1Shape");
        Resource s3Tank2Shape = model.createResource(PRE + "s3Tank2Shape");

        states[3].addProperty(hasShape, s3Shape);

        s3Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank1)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s3Tank1Shape))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank2)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s3Tank2Shape));

        s3Tank1Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h1Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(0.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OnMode));

        s3Tank2Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h2Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(1.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OnMode));

        // s4: Tank1 OffMode h1=0, Tank2 OffMode h2=1
        Resource s4Shape = model.createResource(PRE + "s4Shape");
        Resource s4Tank1Shape = model.createResource(PRE + "s4Tank1Shape");
        Resource s4Tank2Shape = model.createResource(PRE + "s4Tank2Shape");

        states[4].addProperty(hasShape, s4Shape);

        s4Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shTargetClass, State)
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank1)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s4Tank1Shape))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, hasTank2)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shNode, s4Tank2Shape));

        s4Tank1Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h1Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(0.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OffMode));

        s4Tank2Shape.addProperty(RDF.type, model.createResource(SH + "NodeShape"))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, h2Prop)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, model.createTypedLiteral(1.0)))
                .addProperty(shProperty, model.createResource()
                        .addProperty(shPath, mode)
                        .addLiteral(shMinCount, 1).addLiteral(shMaxCount, 1)
                        .addProperty(shHasValue, OffMode));

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

        List<Rule> allRules = Rule.parseRules(rules1 + rules2);

        Reasoner reasoner = new GenericRuleReasoner(allRules);
        reasoner.setDerivationLogging(true);

        InfModel inf = ModelFactory.createInfModel(reasoner, model);


        // Print statements before writing
        StmtIterator iterBefore = model.listStatements();
        while (iterBefore.hasNext()) {
            Statement st = iterBefore.nextStatement();
            Resource  subject   = st.getSubject();
            Property  predicate = st.getPredicate();
            RDFNode   object    = st.getObject();

            System.out.println(subject + " " + predicate + " " + object);
        }

        // Write TTL output
        try (FileOutputStream out = new FileOutputStream("D:\\TTL2\\knowledgeGraphWithSHACL_Tank1_IsNext_Test.ttl")) {
            RDFDataMgr.write(out, inf, RDFFormat.TURTLE_PRETTY);
            System.out.println("Tank system knowledge graph has been successfully written to: D:\\TTL2\\knowledgeGraphWithSHACL_Tank1.ttl");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}