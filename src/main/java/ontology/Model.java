package ontology;

import org.apache.jena.base.Sys;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM_MICRO_RULE_INF;

public class Model {

    public static String MAP_ONTOLOGY_FILE = "MapOntology.owl";

    public static String ROUTE_RULES = "rules/route.rules";

    org.apache.jena.rdf.model.Model mapModel = null;

    OntModel ontologyModel = null;

    Resource entrance = null;

    Reasoner routeReasoner = null;

    public Model()
    {
        mapModel = readModel(MAP_ONTOLOGY_FILE);
        ontologyModel = ModelFactory.createOntologyModel( OWL_MEM_MICRO_RULE_INF, mapModel);

        routeReasoner = createReasonerForInteraction(ROUTE_RULES);


        Resource centralEnter = addPlace("Центральный вход", "   ", 55.1, 55.2);
        centralEnter.addProperty(RDF.type, ontologyModel.getOntClass(OntologyClasses.Map.ENTRANCE));

        Resource node = addNode(55.11,55.22);
        Resource road = addRoad(centralEnter,node,100);
        Resource pavilionArmenia = addPlace("Армения","",55.1,55.2);
        Resource road1 = addRoad(node,pavilionArmenia,60);

        Resource pavilionBelarus = addPlace("Белоруссия","",55.1,55.2);
        Resource road2 = addRoad(centralEnter,pavilionBelarus,150);

        Resource it = addPlace("IT-парк","",55.1,55.2);
        Resource road3 = addRoad(pavilionArmenia,it,250);

        Resource road4 = addRoad(pavilionBelarus,it,300);

        Resource road5 = addRoad(node,pavilionBelarus,120);

        Resource road6 = addRoad(pavilionArmenia,pavilionBelarus,85);

        Resource road7 = addRoad(it,centralEnter,500);

        this.entrance =centralEnter;

        //ontologyModel.write(System.out);
    }

    public Resource addRoad(Resource node1, Resource node2, double length)
    {
        Individual road = ontologyModel.createIndividual(ontologyModel.createResource());
        road.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.ROAD));
        road.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.STARTS_FROM), node1);
        road.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.FINISHES_TO), node2);
        road.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LENGTH), ontologyModel.createTypedLiteral(length));
        return road;
    }

    public Resource addNode(double latitude, double longitude)
    {
        Individual place = ontologyModel.createIndividual(ontologyModel.createResource());
        place.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.NODE));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LATITUDE), ontologyModel.createTypedLiteral(latitude));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LONGITUDE), ontologyModel.createTypedLiteral(longitude));
        return place;
    }

    public Resource addPlace(String name, String description, double latitude, double longitude)
    {
        Individual place = ontologyModel.createIndividual(ontologyModel.createResource());
        place.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.PLACE));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME), ontologyModel.createTypedLiteral(name));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_DESCRIPTION), ontologyModel.createTypedLiteral(description));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LATITUDE), ontologyModel.createTypedLiteral(latitude));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LONGITUDE), ontologyModel.createTypedLiteral(longitude));
        return place;
    }

    public void calcRoute()
    {
        InfModel infModel = ModelFactory.createInfModel(routeReasoner, ontologyModel);
        //infModel.write(System.out);

        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "SELECT ?interrupt " +
                "WHERE { " +
                "?interrupt a ro:InterruptNode ." +
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, infModel);
        ResultSet rs = qExec.execSelect();

        while (rs.hasNext())
        {
            Resource interruptPoint = rs.next().getResource("interrupt");

            Resource point = interruptPoint.getPropertyResourceValue(infModel.getProperty(ObjectProperties.Route.HAS_ACCORDING_OBJECT));



            String name = point.hasProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)) ? point.getProperty(infModel.getProperty(DataProperties.Map.HAS_NAME)).getString() : "";

            System.out.println(interruptPoint+" "+name);
            while (interruptPoint.hasProperty(infModel.getProperty(ObjectProperties.Route.HAS_PREVIOUS)))
            {
                interruptPoint = interruptPoint.getPropertyResourceValue(infModel.getProperty(ObjectProperties.Route.HAS_PREVIOUS));
                point = interruptPoint.getPropertyResourceValue(infModel.getProperty(ObjectProperties.Route.HAS_ACCORDING_OBJECT));
                name = point.hasProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)) ? point.getProperty(infModel.getProperty(DataProperties.Map.HAS_NAME)).getString() : "";


                double time = interruptPoint.getProperty(infModel.getProperty(DataProperties.Route.HAS_TOTAL_TIME)).getDouble();

                System.out.println(interruptPoint+" "+name+"    Время в пути: "+time+" минут");
            }

            System.out.println("=================");
        }

    }

    protected org.apache.jena.rdf.model.Model readModel(String modelFile)
    {
        // create an empty model
        org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
        String inputFileName=modelFile;
        // use the RDFDataMgr to find the input file
        InputStream in = RDFDataMgr.open( inputFileName );
        if (in == null) {
            throw new IllegalArgumentException(
                    "File: " + inputFileName + " not found");
        }

        // read the RDF/XML file
        model.read(in, null);
        return model;
    }

    private Reasoner createReasonerForInteraction(String rulesFile)
    {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(rulesFile);
        String rules = readStream( stream);
        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
        reasoner.setDerivationLogging(true);
        reasoner.setDerivationLogging(true);
        reasoner.bindSchema(ontologyModel);
        return reasoner;
    }

    protected static String readStream(InputStream is) {
        StringBuilder sb = new StringBuilder(512);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            int c = 0;
            while ((c = r.read()) != -1) {
                sb.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }
}
