package ontology;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM_MICRO_RULE_INF;

public class VDNHModel {

    private OntModel ontologyModel;

    private static VDNHModel vdnhModel = null;

    private static String ONTOLOGY_FILE = "foo.rdf";

    public static VDNHModel getModel()
    {
        return (vdnhModel == null) ? new VDNHModel() : vdnhModel;
    }

    private VDNHModel()
    {
        ontologyModel = ModelFactory.createOntologyModel(OWL_MEM_MICRO_RULE_INF,readModel(ONTOLOGY_FILE));
    }

    public boolean hasBusRouteBetweenPlaces(String placeId1, String placeId2)
    {
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "SELECT (COUNT(?s) as ?cnt) " +
                "WHERE { " +
                "?p1 a mo:Place . " +
                "?p2 a mo:Place ." +
                "?p1 mo:hasID \""+placeId1+"\" ."+
                "?p2 mo:hasID \""+placeId2+"\" ."+
                "?p1 mo:hasNearestBusStop ?s ."+
                "?p2 mo:hasNearestBusStop ?s ."+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        return rs.next().getLiteral("cnt").getInt()==0;
        //return true;
    }

    public HashMap<String,String> findBusRoute(String placeId1, String placeId2)
    {
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "SELECT ?name1 ?name2 ?brn1 ?brn2 ?cnt" +
                "WHERE { " +
                "?p1 a mo:Place . " +
                "?p2 a mo:Place ." +
                "?p1 mo:hasID \""+placeId1+"\" ."+
                "?p2 mo:hasID \""+placeId2+"\" ."+
                "?p1 mo:hasNearestBusStop ?s1 ."+
                "?p2 mo:hasNearestBusStop ?s2 ."+
                "?brn1 mo:hasAccordingStation ?s1."+
                "?brn2 mo:hasAccordingStation ?s2."+
                "?brn1 mo:hasReachableBusRouteNode ?brn2 ."+
                "?brn1 mo:ofRoute ?r."+
                "?brn2 mo:ofRoute ?r."+
                "?s1 mo:hasName ?name1 ."+
                "?s2 mo:hasName ?name2 ."+
                "?interval a mo:BusStationsInterval ."+
                "?interval mo:startsFrom ?brn1 ."+
                "?interval mo:finishesTo ?brn2 ."+
                "?interval mo:hasCount ?cnt."+
                "}"+
                "ORDER BY ASC(?cnt) LIMIT 1";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        HashMap<String,String> result = new HashMap<>();

        QuerySolution qs = rs.next();
        result.put("station1",qs.getLiteral("name1").getString());
        result.put("station2",qs.getLiteral("name2").getString());

        Resource brn1 = qs.getResource("brn1");
        Resource brn2 = qs.getResource("brn2");

        int counter = 0;
        while (brn1 != brn2 && brn1 != null)
        {
            Resource busStop = brn1.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION));
            String name = busStop.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getString();
            result.put("point"+(++counter),name);
            brn1 = brn1.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_NEXT_BUS_ROUTE_NODE));

            if (brn1.getProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION)).getResource().getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_ID)).getString()
                    == brn2.getProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION)).getResource().getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_ID)).getString())
                break;
        }

        return result;
    }

    public List<String> findNearestPlacesToStation(String stationId)
    {
        List<String> places = new ArrayList<>();

        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "SELECT ?id " +
                "WHERE { " +
                "?p1 a mo:Place . " +
                "?p1 mo:hasID ?id ."+
                "?p1 mo:hasNearestBusStop ?s ."+
                "?s mo:hasID \""+stationId+"\" ."+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            String id = qs.getLiteral("id").getString();
            places.add(id);
        }
        return places;
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
}
