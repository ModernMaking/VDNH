package ontology;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

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

    public List<List<String>> findAllBusRoutes()
    {
        List<List<String>> routeList = new ArrayList<>();

        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "SELECT ?r " +
                "WHERE { " +
                "?r a mo:BusRoute ."+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            Resource routeResource = qs.getResource("r");

            List<String> route = new ArrayList<>();

            Resource firstBRN = routeResource.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_FIRST_BUS_ROUTE_NODE));
            route.add(getBusStationIdForBRN(firstBRN));
            Resource currBRN = firstBRN;
            while (currBRN.hasProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_NEXT_BUS_ROUTE_NODE)))
            {
                currBRN = currBRN.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_NEXT_BUS_ROUTE_NODE));
                route.add(getBusStationIdForBRN(currBRN));
            }

            routeList.add(route);
        }

        return routeList;
    }

    private String getBusStationIdForBRN(Resource brn)
    {
        Resource station = brn.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION));
        return station.getProperty(ontologyModel.getDatatypeProperty((DataProperties.Map.HAS_ID))).getString();
    }

    public List<String> getAllReachableStationsFrom(String id)
    {
        List<String> stations = new ArrayList<>();

        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "SELECT ?id2 " +
                "WHERE { " +
                "?brn mo:hasAccordingStation ?s ."+
                "?s mo:hasID \""+id+"\" ."+
                "?brn mo:hasReachableBusRouteNode ?brn2 ."+
                "?brn2 mo:hasAccordingStation ?s2 ."+
                "?s2 mo:hasID ?id2 ."+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            String currId = qs.getLiteral("id2").getString();
            stations.add(currId);
        }

        return stations;
    }

    public HashMap<Integer, String> getAllInterestTags()
    {
        HashMap<Integer, String> res = new HashMap<>();

        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "PREFIX to: <http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#> "+
                "SELECT ?id ?name " +
                "WHERE { " +
                "?tag a to:InterestTag ."+
                "?tag to:hasID ?id ."+
                "?tag to:hasName ?name ."+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            String name = qs.getLiteral("name").getString();
            Integer id = qs.getLiteral("id").getInt();
            res.put(id,name);
        }
        return res;
    }

    public List<String> placeIdsByTags(List<Integer> tagIds)
    {
        List<String> result = new ArrayList<>();
        String filterString = "";

        List<String> predicates = new ArrayList<>();
        tagIds.forEach(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                String curr = "?id = "+String.valueOf(integer);
                predicates.add(curr);
            }
        });
        filterString = "( ?id IN ("+String.join(",",predicates)+") )";

        String fs2 = String.join("|",predicates);

        String fs3 = String.join("||",predicates);

        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "PREFIX to: <http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#> "+
                "SELECT DISTINCT ?placeID " +
                "WHERE { " +
                "?place a mo:Place . "+
                "?place mo:hasID ?placeID . "+
                "?place to:hasInterestTag ?tag . "+
                "?tag a to:InterestTag . "+
                "?tag to:hasID ?id . "+
                "FILTER ("+fs3+" ) "+
                //"FILTER (?tag to:hasID \"10\" ) "+
                //filterString + //"FILTER (?tag to:hasID  ) . "+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            String id = qs.getLiteral("placeID").getString();
            result.add(id);
        }
        return result;
    }

    public List<String> findPlacesSimilarTo(String placeId)
    {
        List<String> placeIds = new ArrayList<>();

        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "PREFIX to: <http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#> "+



                "SELECT ?id2  (SUM(?sim)/COUNT(?sim) as ?msim)" +
                "WHERE { " +
                "?place1 a mo:Place . "+
                "?place1 mo:hasID \""+placeId+"\" . "+
                "?place1 to:hasInterestTag ?tag1 . "+
                "?place2 a mo:Place . "+
                "?place2 mo:hasID ?id2 ."+
                "?place2  to:hasInterestTag ?tag2."+
                "?s a to:TagSimilarity . "+
                "?s to:hasTag1 ?tag1 ."+
                "?s to:hasTag2 ?tag2 ."+
                "?s to:hasSimilarity ?sim ."+
                //"FILTER(?sim>0.4)"+
                //"FILTER (?tag to:hasID \"10\" ) "+
                //filterString + //"FILTER (?tag to:hasID  ) . "+
                "}"+
                //"FILTER(?msim>0.4) "+
                "GROUP BY ?id2";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            String id = qs.getLiteral("id2").getString();
            Double msim = qs.getLiteral("msim").getDouble();
            if (msim>0.4)
                placeIds.add(id);
        }

        return placeIds;
    }

    public HashMap<String,HashMap<String,Double>> getTagSimilarityHeatMap()
    {
        HashMap<String,HashMap<String,Double>> res = new HashMap<>();
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "PREFIX to: <http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#> "+
                "SELECT ?name1 ?name2 ?sim " +
                "WHERE { " +
                "?tag1 a to:InterestTag . "+
                "?tag1 to:hasName ?name1 . "+
                "?tag2 a to:InterestTag . "+
                "?tag2 to:hasName ?name2 . "+
                "?s a to:TagSimilarity . "+
                "?s to:hasTag1 ?tag1 ."+
                "?s to:hasTag2 ?tag2 ."+
                "?s to:hasSimilarity ?sim ."+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            String name1 = qs.getLiteral("name1").getString();
            String name2 = qs.getLiteral("name2").getString();
            Double sim = qs.getLiteral("sim").getDouble();
            if (!res.containsKey(name1))
                res.put(name1, new HashMap<>());
            res.get(name1).put(name2,sim);
        }
        return res;
    }

    public List<String> findRouteAsPlaceIdsBetweenPlaces(String placeId1, String placeId2)
    {
       List<List<List<String>>> search = new ArrayList<>();

       List<List<String>> firstLevel = new ArrayList<>();
       List<String> simplePath = new ArrayList<>();
       simplePath.add(placeId1);
       firstLevel.add(simplePath);
       search.add(firstLevel);

        Set<String> visited = new HashSet<>();
        visited.add(placeId1);

       int level = 0;
       while (level<300)
       {
           level++;
           List<List<String>> currLevel = new ArrayList<>();
           search.add(currLevel);
           List<List<String>> prevLevel = search.get(level-1);

           for (List<String> path: prevLevel)
           {
               String lastPoint = path.get(path.size()-1);
               String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                       "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                       "PREFIX to: <http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#> "+
                       "SELECT ?id2 " +
                       "WHERE { " +
                       "?place a mo:Place . "+
                       "?place mo:hasID \""+lastPoint+"\" . "+
                       "?place mo:hasLatitude ?lat . "+
                       "?place mo:hasLongitude ?lon . "+
                       "?dist a mo:Distance ."+
                       "?dist mo:startsFrom ?place ."+
                       "?dist mo:finishesTo ?place2 . "+
                       "?place2 mo:hasID ?id2 . "+
                       "?place2 mo:hasLatitude ?lat2 . "+
                       "?place2 mo:hasLongitude ?lon2 . "+

                       "}";
               Query query = QueryFactory.create(queryString);
               QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
               ResultSet rs = qExec.execSelect();
               while (rs.hasNext())
               {
                   QuerySolution qs = rs.next();
                   String id2 = qs.getLiteral("id2").getString();
                   List<String> path2 = new ArrayList<>(path);//.subList(0, path.size()-1);


                   if (!visited.contains(id2) && !path2.contains(id2))
                   {
                       visited.add(id2);
                       path2.add(id2);
                       currLevel.add(path2);
                       if (id2.equals(placeId2))
                           return path2;
                   }

               }
           }

       }

        return new ArrayList<>();
    }

    public class RouteNode
    {
        private LocalDateTime startDateTime;
        private LocalDateTime finishDateTime;
        private String placeIdStart;
        private String placeIdFinish;
        private List<String> otherPlaces;
        private int durationMins;
        private int totalMins;
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
