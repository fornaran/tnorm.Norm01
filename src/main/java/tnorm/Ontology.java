package tnorm;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.datatypes.xsd.XSDDuration;
import com.hp.hpl.jena.datatypes.xsd.impl.XSDDateTimeType;
import com.hp.hpl.jena.datatypes.xsd.impl.XSDDouble;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.ontology.impl.OntModelImpl;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import com.hp.hpl.jena.reasoner.rulesys.*;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.jena.graph.loader.DefaultGraphLoader;


public class Ontology {
    private final static String modelSourceFile = "src/main/java/tnorm/eventZtl.owl";
    private final static String modelWriteFile = "src/main/java/tnorm/eventToWrite.owl";
    private final static String rulesSourceFile = "src/main/java/tnorm/myRules.rules";

    private final static String ns = "http://www.people.usi.ch/fornaran/ontology/event#";
    private final static String schema = "http://schema.org/";
    private final static String time = "http://www.w3.org/2006/time#";

    private final static Now now = new Now("2020-05-18T10:00:00Z");
    private final static Salience salience = new Salience();
    private final static int maxSalience = salience.getMaxSalience();
    private final static OntModel rawModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
    private final GenericRuleReasoner reasoner;

    private final static Counters counters = new Counters();
    //private final static Now now = new Now();

    // ========== Conceptual Model ==========
    private final OntClass deonticRelation;
    private final OntClass restrictedTrafficAreaAccess; //access0n
    private final OntClass agent; //agent0n
    private final OntClass timeInstant; //deadlineObl0n_0n, te0n (time event), timeCreationObl, tPayment
    private final OntClass paymentClass; //paymentObl0n_n
    private final OntClass vehicleClass;
    private final OntClass ambulanceClass;

    //Properties
    private final OntProperty activated;
    private final OntProperty inXSDDateTime;
    private final OntProperty atTime;
    private final OntProperty vehicleOwner;
    private final OntProperty creationTime;
    private final OntProperty timeEnd;
    private final OntProperty fulfilled;
    private final OntProperty violated;
    private final OntProperty payed;
    private final OntProperty hasVehicle;
    private final OntProperty reason;

    private final Individual vehicle01;
    private final Individual vehicle02;
    private final Individual billyCar;

    Ontology() {

        OntDocumentManager dm =  rawModel.getDocumentManager();
        dm.addAltEntry( "http://schema.org/version/latest/schema.rdf",
                "file: src\\main\\java\\tnorm\\schema.rdf" );
        dm.addAltEntry( "http://www.w3.org./2006/time#2016",
                "file: src\\main\\java\\tnorm\\time.owl" );

        rawModel.read("file:" + modelSourceFile);

        createBuiltIn();
        List<Rule> rules = Rule.rulesFromURL(rulesSourceFile);
        reasoner = new GenericRuleReasoner(rules);
        reasoner.setOWLTranslation(true);
        reasoner.setTransitiveClosureCaching(true);
        reasoner.setFunctorFiltering(true);

        // ========== Conceptual Model ==========
        deonticRelation = rawModel.getOntClass(ns + "DeonticRelation");
        restrictedTrafficAreaAccess =  rawModel.getOntClass(ns + "RestrictedTrafficAreaAccess"); //access0n
        agent =  rawModel.getOntClass(schema + "Person"); //agent0n
        timeInstant =  rawModel.getOntClass(time + "Instant"); //deadlineObl0n_0n, te0n (time event), timeCreationObl, tPayment
        paymentClass =  rawModel.getOntClass(ns + "Payment"); //paymentObl0n_n
        vehicleClass =  rawModel.getOntClass(schema + "Vehicle");
        ambulanceClass =  rawModel.getOntClass(ns + "Ambulance");

        OntClass nowClass = rawModel.createClass(ns + "Now");
        nowClass.addSuperClass(timeInstant);

        //Properties
        activated = rawModel.getOntProperty(ns + "activated");
        inXSDDateTime =  rawModel.getOntProperty( time + "inXSDDateTimeStamp");
        atTime =  rawModel.getOntProperty(ns + "atTime");
        vehicleOwner =  rawModel.getOntProperty(ns + "vehicleOwner");
        creationTime = rawModel.getOntProperty(ns + "creationTime");
        timeEnd = rawModel.getOntProperty(ns + "timeEnd");
        fulfilled = rawModel.getOntProperty(ns+ "fulfilled");
        violated = rawModel.getOntProperty(ns + "violated");
        payed = rawModel.getOntProperty(ns + "payed");
        hasVehicle = rawModel.getOntProperty(ns + "hasVehicle");
        reason = rawModel.getOntProperty(ns + "reason");

        //Individual
        Individual billy = rawModel.createIndividual(ns + "Billy", agent);
        //billyCar = rawModel.createIndividual(ns + "BillyCar", vehicleClass);
        billyCar = rawModel.createIndividual(ns + "BillyCar", ambulanceClass);
        billyCar.addProperty(vehicleOwner, billy);
        Individual agent01 = rawModel.createIndividual(ns + "agent01", agent);
        vehicle01 = rawModel.createIndividual(ns + "vehicle01", vehicleClass);
        vehicle01.addProperty(vehicleOwner, agent01);
        Individual agent02 = rawModel.createIndividual(ns + "agent02", agent);
        vehicle02 = rawModel.createIndividual(ns + "vehicle02", vehicleClass);
        vehicle02.addProperty(vehicleOwner, agent02);

        try{
            rawModel.write(new FileOutputStream(modelWriteFile));
        } catch (Exception e){
            System.out.println("!!!!! Error while updating the ontology !!!!!");
            e.printStackTrace();
        }
        rawModel.read("file:" + modelWriteFile);
        setLog();
        //OntClass Ambulance = null;
    }

    // Method to create the ontology at the beginning of the program
    public void createOntology() {

        //Restricted Traffic Area Access
        Individual access1 = rawModel.createIndividual(ns + "access1", restrictedTrafficAreaAccess);
        Individual tAccess1 = rawModel.createIndividual(ns + "tAccess1", timeInstant);
        tAccess1.addProperty(inXSDDateTime, rawModel.createTypedLiteral("2020-05-19T11:00:00Z", XSDDatatype.XSDdateTime));
        access1.addProperty(atTime, tAccess1);
        //access1.addProperty(hasVehicle, vehicle01);
        access1.addProperty(hasVehicle, billyCar);
        counters.addOneToRTAACounter();

        Individual access2 = rawModel.createIndividual(ns + "access2", restrictedTrafficAreaAccess);
        Individual tAccess2 = rawModel.createIndividual(ns + "tAccess2", timeInstant);
        tAccess2.addProperty(inXSDDateTime, rawModel.createTypedLiteral("2020-05-20T11:00:00Z", XSDDatatype.XSDdateTime));
        access2.addProperty(atTime, tAccess2);
        access2.addProperty(hasVehicle, vehicle02);
        counters.addOneToRTAACounter();

        Individual access3 = rawModel.createIndividual(ns + "access3", restrictedTrafficAreaAccess);
        Individual tAccess3 = rawModel.createIndividual(ns + "tAccess3", timeInstant);
        tAccess3.addProperty(inXSDDateTime, rawModel.createTypedLiteral("2020-05-31T11:00:00Z", XSDDatatype.XSDdateTime));
        access3.addProperty(atTime, tAccess3);
        access3.addProperty(hasVehicle, vehicle01);
        counters.addOneToRTAACounter();

        Individual access4 = rawModel.createIndividual(ns + "access4", restrictedTrafficAreaAccess);
        Individual tAccess4 = rawModel.createIndividual(ns + "tAccess4", timeInstant);
        tAccess4.addProperty(inXSDDateTime, rawModel.createTypedLiteral("2020-08-09T11:30:00Z", XSDDatatype.XSDdateTime));
        access4.addProperty(atTime, tAccess4);
        access4.addProperty(hasVehicle, vehicle01);
        counters.addOneToRTAACounter();

        Individual pay3 = rawModel.createIndividual(ns + "pay3", paymentClass);
        Individual tPayment = rawModel.createIndividual(ns + "tPayment3", timeInstant);
        tPayment.addProperty(inXSDDateTime, rawModel.createTypedLiteral("2020-05-31T12:45:00Z", XSDDatatype.XSDdateTime));
        pay3.addProperty(atTime, tPayment);
        pay3.addProperty(reason, access3);

        //Agents
        Individual agent1 = rawModel.createIndividual(ns + "agent1", agent);
        Individual agent2 = rawModel.createIndividual(ns + "agent2", agent);

        //Vehicles
        Individual vehicle1 = rawModel.createIndividual(ns + "vehicle1", vehicleClass);
        vehicle1.addProperty(vehicleOwner, agent1);
        Individual vehicle2 = rawModel.createIndividual(ns + "vehicle2", vehicleClass);
        vehicle2.addProperty(vehicleOwner, agent2);

        try{
            rawModel.write(new FileOutputStream(modelWriteFile));
        } catch (Exception e){
            System.out.println("!!!!! Error while writing the ontology !!!!!");
        }
    }

    public void addRestrictTrafficAreaAccess(String time, String vehicleName){

        String counter = String.valueOf(counters.getRTAACounter());
        counters.addOneToRTAACounter();
        Individual access = rawModel.createIndividual(ns + "access" + counter, restrictedTrafficAreaAccess);
        Individual tAccess = rawModel.createIndividual(ns + "tAccess" + counter, timeInstant);
        tAccess.addProperty(inXSDDateTime, rawModel.createTypedLiteral(time, XSDDatatype.XSDdateTime));
        access.addProperty(atTime, tAccess);
        Individual vehicle;
        if(rawModel.getIndividual(ns + vehicleName ) != null){
            vehicle = rawModel.getIndividual(ns + vehicleName );
        } else {
            vehicle = rawModel.createIndividual(ns + vehicleName, vehicleClass);
        }
        access.addProperty(hasVehicle, vehicle);

    }

    public void addPayment(String time, int accessToPayFor){
        String accessToPayForName = null;
        Map<String, String> accesses = getPayableAccesses();
        int i = 1;
        for(Map.Entry<String,String> entry : accesses.entrySet()) {
            if(i == accessToPayFor){
                accessToPayForName = entry.getKey();
            }
            i++;
        }

        String counter = String.valueOf(counters.getPaymentCounter());
        counters.addOneToPaymentCounter();
        Individual payment = rawModel.createIndividual(ns + "payment" + counter, paymentClass);
        Individual tPayment = rawModel.createIndividual(ns + "tPayment" + counter, timeInstant);
        tPayment.addProperty(inXSDDateTime, rawModel.createTypedLiteral(time, XSDDatatype.XSDdateTime));
        payment.addProperty(atTime, tPayment);
        Individual paymentAccess = rawModel.getIndividual(ns + accessToPayForName);
        payment.addProperty(reason, paymentAccess);
        paymentAccess.addProperty(payed, payment);
    }

    public void printRestrictedTrafficAreaAccesses(){
        Map<String, String> accesses = getPayableAccesses();
        int i = 1;
        for(Map.Entry<String,String> entry : accesses.entrySet()) {
            System.out.println("\t" + i +". " + entry.getKey() + ": " + entry.getValue());
            i++;
        }
    }

    public void updateOntology() {

        for(int i = maxSalience; i >= 0; i--) {
            salience.setSalience(i);
            OntModel model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, rawModel);

            InfModel infModel = ModelFactory.createInfModel(reasoner, model);
            rawModel.add(infModel.getDeductionsModel().listStatements());

            System.out.println("================  Salience " + i + " ==================");
            for(StmtIterator it = infModel.getDeductionsModel().listStatements(); it.hasNext();) {
                System.out.println(it.next());
            }
            System.out.println("===============================================");
        }

        try{
            rawModel.write(new FileOutputStream(modelWriteFile));
        } catch (Exception e){
            System.out.println("!!!!! Error while updating the ontology !!!!!");
            e.printStackTrace();
        }
    }

    public void printOntology(){

        System.out.println("\n\n================================================================================");
        System.out.println("=== Now: " + now.getNow());

        System.out.println("\n=== Restricted Traffic Area Accesses ===");
        Map<String, String> accesses = new TreeMap<>();
        for(ExtendedIterator<? extends OntResource> i = restrictedTrafficAreaAccess.listInstances(); i.hasNext();){
            OntResource access = i.next();
            Individual tAccess = rawModel.getIndividual(access.getPropertyValue(atTime).toString());
            String accessName = access.getLocalName();
            String accessTime = tAccess.getPropertyValue(inXSDDateTime).toString();
            accessTime = accessTime.substring(0, accessTime.indexOf("^"));
            accesses.put(accessName, accessTime);
        }

        for(Map.Entry<String,String> entry : accesses.entrySet()) {
            System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
        }

        //OntClass deonticRelation = rawModel.getOntClass(ns + "DeonticRelation");
        if(!deonticRelation.listInstances().toList().isEmpty()) {
            Map<String, String> deonticRelationsCreations = new TreeMap<>();
            Map<String, String> deonticRelationsDeadlines = new TreeMap<>();
            Map<String, String> deonticRelationsActivations = new TreeMap<>();
            Map<String, String> deonticRelationsFulfillment = new TreeMap<>();
            Map<String, String> deonticRelationsViolations = new TreeMap<>();
            for(ExtendedIterator<? extends OntResource> i = deonticRelation.listInstances(); i.hasNext();){
                OntResource deontic = i.next();
                String deonticName = deontic.getLocalName();
                Individual creation = rawModel.getIndividual(deontic.getPropertyValue(creationTime).toString());
                String tCreation = creation.getPropertyValue(inXSDDateTime).toString();
                tCreation = tCreation.substring(0, tCreation.indexOf("Z"));
                Individual teEnd = rawModel.getIndividual(deontic.getPropertyValue(timeEnd).toString());
                String tDeadline = teEnd.getPropertyValue(inXSDDateTime).toString();
                tDeadline = tDeadline.substring(0, tDeadline.indexOf("Z"));
                Individual activation = rawModel.getIndividual(deontic.getPropertyValue(activated).toString());
                deonticRelationsCreations.put(deonticName,tCreation);
                deonticRelationsDeadlines.put(deonticName, tDeadline);
                deonticRelationsActivations.put(deonticName, activation.getLocalName());

                RDFNode deonticFulfillment = deontic.getPropertyValue(fulfilled);
                if(deonticFulfillment != null) {
                    String fulfillment = deonticFulfillment.toString();
                    fulfillment = fulfillment.substring(0, fulfillment.indexOf("Z"));
                    deonticRelationsFulfillment.put(deonticName, fulfillment);
                } else {
                    RDFNode deonticViolation = deontic.getPropertyValue(violated);
                    if(deonticViolation != null){
                        String violation = deonticViolation.toString();
                        violation = violation.substring(0, violation.indexOf("Z"));
                        deonticRelationsViolations.put(deonticName, violation);
                    }
                }


            }
            System.out.println("\n=== Deontic Relation ===");
            for(Map.Entry<String,String> entry : deonticRelationsCreations.entrySet()) {
                System.out.print("\t" + entry.getKey() + ": ");
                System.out.print(deonticRelationsActivations.get(entry.getKey()));
                System.out.print("\t" + entry.getValue());
                System.out.println("\t" + deonticRelationsDeadlines.get(entry.getKey()));
            }

            System.out.println("\n=== Payments ===");
            Map<String, String> payments = new TreeMap<>();
            Map<String, String> reasons = new TreeMap<>();
            if(!paymentClass.listInstances().toList().isEmpty()) {
                for (ExtendedIterator<? extends OntResource> i = paymentClass.listInstances(); i.hasNext(); ) {
                    OntResource payment = i.next();
                    Individual tPayment = rawModel.getIndividual(payment.getPropertyValue(atTime).toString());
                    String paymentName = payment.getLocalName();
                    Individual access = rawModel.getIndividual(payment.getPropertyValue(reason).toString());
                    String paymentTime = tPayment.getPropertyValue(inXSDDateTime).toString();
                    paymentTime = paymentTime.substring(0, paymentTime.indexOf("Z"));
                    payments.put(paymentName, paymentTime);
                    reasons.put(paymentName, access.getLocalName());
                }
                for (Map.Entry<String, String> entry : payments.entrySet()) {
                    System.out.print("\t" + entry.getKey() + ": ");
                    System.out.print(reasons.get(entry.getKey()));
                    System.out.println("\t" + entry.getValue());
                }
            }

            System.out.println("\n=== Deontic Relation fulfillment ===");
            for(Map.Entry<String,String> entry : deonticRelationsFulfillment.entrySet()) {
                if(!entry.getValue().equals("Thing")) {
                    System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
                }
            }System.out.println("\n=== Deontic Relation Violations ===");
            for(Map.Entry<String,String> entry : deonticRelationsViolations.entrySet()) {
                System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
            }
        }
        System.out.println("================================================================================\n\n");
    }

    int getOblCounterAndAddOne(){
        int counter = counters.getObli01counter();
        counters.addOneToObli01counter();
        return counter;
    }

    public static int getSalience() {
        return salience.getSalience();
    }

    public static String getNow(){
        return now.getNow();
    }

    public static void setNow(String value){
        now.setNow(value);
    }

    private Map<String, String > getPayableAccesses(){
        Map<String, String> accesses = new TreeMap<>();
        for(ExtendedIterator<? extends OntResource> i = deonticRelation.listInstances(); i.hasNext();){
            OntResource deontic = i.next();
            RDFNode deonticActivation = deontic.getPropertyValue(activated);

            RDFNode deonticFulfillment = deontic.getPropertyValue(fulfilled);
            RDFNode deonticViolation = deontic.getPropertyValue(violated);
            if(deonticFulfillment == null && deonticViolation == null) {
                Individual access = rawModel.getIndividual(deonticActivation.toString());
                Individual tAccess = rawModel.getIndividual(access.getPropertyValue(atTime).toString());
                String accessName = access.getLocalName();
                String accessTime = tAccess.getPropertyValue(inXSDDateTime).toString();
                accessTime = accessTime.substring(0, accessTime.indexOf("Z"));
                accesses.put(accessName, accessTime);
            }
        }
        return accesses;
    }

    private void createBuiltIn(){
        /* This built-in return true if the salience level is equal to the one specified by the parameter n*/
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "isSalience";
            }

            @Override
            public int getArgLength() {
                return 1;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                Node n1 = getArg(0, args, context);
                if (n1.isLiteral()) {
                    Object v1 = n1.getLiteral().getValue();
                    if (v1 instanceof Integer) {
                        int n = (int) v1;
                        int salience = Ontology.getSalience();
                        return n == salience;
                    }
                }
                return false;
            }

        });

        /* This built in add one to the global counter and return the new value
         * in order to have different name for each obligation */
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "getCounter";
            }

            @Override
            public int getArgLength() {
                return 1;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                BindingEnvironment env = context.getEnv();
                int value = getOblCounterAndAddOne();
                Node counterValue = NodeFactory.createLiteral(String.valueOf(value), null, XSDDouble.XSDinteger);
                return env.bind(args[0], counterValue);
            }

        });

        /* This built in check if the access time is
         * between 7am and 7pm*/
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "checkTime";
            }

            @Override
            public int getArgLength() {
                return 1;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                Node n1 = getArg(0, args, context);

                if (n1.isLiteral()) {

                    Object v1 = n1.getLiteral().getValue();

                    if (v1 instanceof XSDDateTime) {
                        XSDDateTime nv1 = (XSDDateTime) v1;
                        Calendar cal = nv1.asCalendar();
                        return cal.get(Calendar.HOUR_OF_DAY) >= 7 && cal.get(Calendar.HOUR_OF_DAY) < 19;
                    }
                }
                return false;
            }

        });

        /* This built in check if it Christmas or not */
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "isChristmas";
            }

            @Override
            public int getArgLength() {
                return 1;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                Node n1 = getArg(0, args, context);

                if (n1.isLiteral()) {

                    Object v1 = n1.getLiteral().getValue();

                    if (v1 instanceof XSDDateTime) {
                        XSDDateTime nv1 = (XSDDateTime) v1;
                        Calendar cal = nv1.asCalendar();
                        return cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) == 25;
                    }
                }
                return false;
            }

        });

        /* The checkDeadline builtin is used to compute the deadline of an obligation on the basis of its activation time
         * and its duration interval */
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "addDeadline";
            }

            @Override
            public int getArgLength() {
                return 3;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                BindingEnvironment env = context.getEnv();
                Node n1 = getArg(0, args, context);
                Node n2 = getArg(1, args, context);

                if (n1.isLiteral() && n2.isLiteral()) {

                    Object v1 = n1.getLiteral().getValue();
                    Object v2 = n2.getLiteralValue();

                    if (v1 instanceof XSDDateTime && v2 instanceof XSDDuration) {

                        XSDDateTime nv1 = (XSDDateTime) v1;
                        XSDDuration nv2 = (XSDDuration) v2;
                        Calendar cal = nv1.asCalendar();

                        cal.add(Calendar.YEAR, nv2.getYears());
                        cal.add(Calendar.MONTH, nv2.getMonths());
                        cal.add(Calendar.DATE, nv2.getDays());
                        cal.add(Calendar.HOUR_OF_DAY, nv2.getHours());
                        cal.add(Calendar.MINUTE, nv2.getMinutes());
                        cal.add(Calendar.SECOND, nv2.getFullSeconds());

                        nv1 = new XSDDateTime(cal);

                        Node sum1 = NodeFactory.createLiteral(nv1.toString(), null, XSDDateTimeType.XSDdateTime);

                        return env.bind(args[2], sum1);
                    }
                }
                return false;
            }
        });

        /* This built in check if the event time is greater or equal then now*/
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "greaterThanNow";
            }

            @Override
            public int getArgLength() {
                return 1;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                Node n1 = getArg(0, args, context);

                String now = Ontology.getNow();

                if (n1.isLiteral()) {

                    Object v1 = n1.getLiteral().getValue();
                    if (v1 instanceof XSDDateTime) {
                        XSDDateTime nv1 = (XSDDateTime) v1;

                        LocalDateTime dateTime = LocalDateTime.of(
                                nv1.getYears(),
                                nv1.getMonths(),
                                nv1.getDays(),
                                nv1.getHours(),
                                nv1.getMinutes(),
                                (int) nv1.getSeconds());

                        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                        LocalDateTime nowDateTime = LocalDateTime.parse(now.substring(0, now.length()-1), dateFormat);
                        return nowDateTime.compareTo(dateTime) >= 0;
                    }
                }
                return false;
            }
        });

        /* This built in check if a event time t1 is greater or equal then a time event t2*/
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "teGreaterThan";
            }

            @Override
            public int getArgLength() {
                return 2;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                Node n1 = getArg(0, args, context);
                Node n2 = getArg(1, args, context);

                if (n1.isLiteral() && n2.isLiteral()) {

                    Object v1 = n1.getLiteral().getValue();
                    Object v2 = n2.getLiteral().getValue();
                    if (v1 instanceof XSDDateTime && v2 instanceof XSDDateTime) {
                        XSDDateTime nv1 = (XSDDateTime) v1;
                        XSDDateTime nv2 = (XSDDateTime) v2;

                        LocalDateTime dateTime1 = LocalDateTime.of(
                                nv1.getYears(),
                                nv1.getMonths(),
                                nv1.getDays(),
                                nv1.getHours(),
                                nv1.getMinutes(),
                                (int) nv1.getSeconds());

                        LocalDateTime dateTime2 = LocalDateTime.of(
                                nv2.getYears(),
                                nv2.getMonths(),
                                nv2.getDays(),
                                nv2.getHours(),
                                nv2.getMinutes(),
                                (int) nv2.getSeconds());
                        return dateTime1.compareTo(dateTime2) >= 0;
                    }
                }
                return false;
            }
        });

        /* This built in return Now as XSDDateTime*/
        BuiltinRegistry.theRegistry.register(new BaseBuiltin() {
            @Override
            public String getName() {
                return "getNow";
            }

            @Override
            public int getArgLength() {
                return 0;
            }

            @Override
            public boolean bodyCall(Node[] args, int length, RuleContext context) {
                checkArgs(length, context);
                BindingEnvironment env = context.getEnv();
                Node sum1 = NodeFactory.createLiteral(Ontology.getNow(), null, XSDDateTimeType.XSDdateTime);
                return env.bind(args[0], sum1);
            }
        });
    }

    private void setLog(){
        Logger log = Logger.getLogger( KnowledgeBase.class.getName());
        log.setLevel(Level.OFF);
        log = Logger.getLogger( DefaultGraphLoader.class.getName());
        log.setLevel(Level.OFF);
        log = Logger.getLogger(OntModelImpl.class.getName());
        log.setLevel(Level.OFF);
        log = Logger.getLogger(OntModel.class.getName());
        log.setLevel(Level.OFF);
        log = Logger.getLogger(RDFDefaultErrorHandler.class.getName());
        log.setLevel(Level.OFF);
    }
}

