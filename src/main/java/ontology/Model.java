package ontology;

import com.example.vdnh.model.Place;
import com.github.jsonldjava.utils.Obj;
import com.google.gson.Gson;
import io.swagger.models.auth.In;
import javassist.compiler.ast.Pair;
import org.apache.jena.base.Sys;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import static java.lang.Math.cos;
import static org.apache.jena.ontology.OntModelSpec.OWL_MEM_MICRO_RULE_INF;

public class Model {

    public static String MAP_ONTOLOGY_FILE = "MapOntology.owl";

    public static String TAG_ONTOLOGY_FILE = "TagOntology.owl";

    public static String REQUEST_ONTOLOGY_FILE = "RequestOntology.owl";

    public static String ROUTE_RULES = "rules/route.rules";

    public static String INTERESTED_PLACES_RULES = "rules/interestPlaces.rules";

    public static String BUS_RULES = "rules/bus.rules";

    org.apache.jena.rdf.model.Model mapModel = null;

    org.apache.jena.rdf.model.Model tagModel = null;

    org.apache.jena.rdf.model.Model requestModel = null;

    OntModel ontologyModel = null;

    Resource entrance = null;

    Reasoner routeReasoner = null;

    Reasoner interestedPlacesReasoner = null;

    Reasoner busReasoner = null;

    HashMap<String,Resource> places = new HashMap<>();

    HashMap<Integer,Resource> tags = new HashMap<>();

    HashMap<String, HashMap<String,Double>> distances = new HashMap<>();

    public void addInterests()
    {
        JSONParser parser = null;
        try {
            parser = new JSONParser(new FileReader(getClass().getResource("/").getPath() + "../classes/tags.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Object obj = null;//parseObject();//parse();//parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
        try {
            obj = parser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String jsonInString = new Gson().toJson(obj);
        JSONArray mJSONArray = new JSONArray(jsonInString);
        addTagsFromJSONArray(mJSONArray);
    }

    public void addTagsFromJSONArray(JSONArray array)
    {
        array.forEach(new Consumer<Object>() {
            @Override
            public void accept(Object o) {
                //System.out.println("INTEREST: "+((JSONObject)o).get("id"));
                JSONObject object = ((JSONObject)o);
                addInterestTag(object.getInt("id"), object.getString("name"));
            }

            @Override
            public Consumer<Object> andThen(Consumer<? super Object> after) {
                return Consumer.super.andThen(after);
            }
        });
    }

    public void addPlaces()
    {
        JSONParser parser = null;
        try {
            parser = new JSONParser(new FileReader(getClass().getResource("/").getPath() +"../classes/export.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        Object obj = null;//parseObject();//parse();//parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
        try {
            obj = parser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String jsonInString = new Gson().toJson(obj);
        JSONObject mJSONObject = new JSONObject(jsonInString);
        //System.out.println(mJSONObject);

        Set<String> keySet = mJSONObject.keySet();
        //System.out.println("Events: "+keySet);



        JSONObject places = (mJSONObject.getJSONObject("places"));
        for (String placeId: places.keySet())
        {
            String title = "";
            String type = "";
            double latitude = 0;
            double longitude = 0;

            //System.out.println("ID: "+placeId);
            if (places.getJSONObject(placeId).has("title"))
                title=places.getJSONObject(placeId).getString("title");

            if (places.getJSONObject(placeId).has("type"))
                type = places.getJSONObject(placeId).getString("type");

            if (places.getJSONObject(placeId).has("coordinates"))
            {
                JSONArray coords = places.getJSONObject(placeId).getJSONArray("coordinates");
                //System.out.println("long: "+coords.getBigDecimal(0)+" lat:"+coords.getBigDecimal(1));
                latitude = coords.getBigDecimal(1).doubleValue();
                longitude = coords.getBigDecimal(0).doubleValue();
            }


            Resource place = addPlace(placeId,title," ",latitude,longitude);
            if (type.equals("Вход"))
            {
                place.addProperty(RDF.type, ontologyModel.getOntClass(OntologyClasses.Map.ENTRANCE));
            }
            if (type.equals("Остановка"))
            {
                place.addProperty(RDF.type, ontologyModel.getOntClass(OntologyClasses.Map.BUS_STOP));
            }


            //Place place = new Place(Integer.valueOf(placeId).longValue(),title, type, latitude, longitude);
            //placeRepository.save(place);
        }


        JSONObject events = mJSONObject.getJSONObject("events");
        for (String eventId: events.keySet()) {
            JSONObject event = events.getJSONObject(eventId);
            String previewtext = event.getString("preview_text");
            String title = event.getString("title");
            JSONArray eventPlaces = event.getJSONArray("places");
            System.out.println(event);
        }
        System.out.println("EVENTS "+events.toString());

    }

    public void addTagsToPlaces()
    {
        JSONParser parser = null;
        try {
            parser = new JSONParser(new FileReader(getClass().getResource("/").getPath() +"../classes/place_tags.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        Object obj = null;//parseObject();//parse();//parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
        try {
            obj = parser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String jsonInString = new Gson().toJson(obj);
        JSONObject mJSONObject = new JSONObject(jsonInString);

        for (String placeId : mJSONObject.keySet())
        {
            Resource place = places.get(placeId);
            JSONArray tagsForPlace = mJSONObject.getJSONArray(placeId);
            tagsForPlace.forEach(new Consumer<Object>() {
                @Override
                public void accept(Object o) {
                    Integer tagId = Integer.parseInt(o.toString());
                    Resource tag = tags.get(tagId);
                    addInterestTagToPlace(place,tag);
                }

                @Override
                public Consumer<Object> andThen(Consumer<? super Object> after) {
                    return Consumer.super.andThen(after);
                }
            });
        }

    }


    public void addTagsSimilarities()
    {
        JSONParser parser = null;
        try {
            parser = new JSONParser(new FileReader(getClass().getResource("/").getPath() +"../classes/similarities.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        Object obj = null;//parseObject();//parse();//parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
        try {
            obj = parser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String jsonInString = new Gson().toJson(obj);
        JSONObject mJSONObject = new JSONObject(jsonInString);

        for (String tagId1: mJSONObject.keySet())
        {
            JSONObject tagSimilarities = mJSONObject.getJSONObject(tagId1);
            for (String tagId2: tagSimilarities.keySet())
            {
                double similarity = tagSimilarities.getDouble(tagId2);
                Resource tag1 = tags.get(Integer.parseInt(tagId1));
                Resource tag2 = tags.get(Integer.parseInt(tagId2));
                addTagSimilarity(tag1,tag2,similarity);
            }
        }
    }


    public void addBusRoutes()
    {
        JSONParser parser = null;
        try {
            parser = new JSONParser(new FileReader(getClass().getResource("/").getPath() +"../classes/bus_routes.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        Object obj = null;//parseObject();//parse();//parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
        try {
            obj = parser.parse();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String jsonInString = new Gson().toJson(obj);
        JSONArray mJSONarray = new JSONArray(jsonInString);

        mJSONarray.forEach(new Consumer<Object>() {
            @Override
            public void accept(Object o) {
                JSONArray busStops = (JSONArray) o;
                List<String> stations = new ArrayList<>();
                busStops.forEach(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        stations.add(o.toString());
                    }

                    @Override
                    public Consumer<Object> andThen(Consumer<? super Object> after) {
                        return Consumer.super.andThen(after);
                    }
                });
                addBusRoute(stations);
            }

            @Override
            public Consumer<Object> andThen(Consumer<? super Object> after) {
                return Consumer.super.andThen(after);
            }
        });

        InfModel infModel = ModelFactory.createInfModel(busReasoner, ontologyModel);
        ontologyModel.add(infModel.difference(ontologyModel));

    }






    private static boolean isCreated = false;

    private static Model model = null;

    public static Model getModel()
    {
        if (isCreated)
        {
            return model;
        }
        else
        {
            model = new Model();
            isCreated=true;
        }
        return model;
    }

    private Model()
    {
        mapModel = readModel(MAP_ONTOLOGY_FILE);
        tagModel = readModel(TAG_ONTOLOGY_FILE);
        requestModel = readModel(REQUEST_ONTOLOGY_FILE);

        ontologyModel = ModelFactory.createOntologyModel(OWL_MEM_MICRO_RULE_INF, ModelFactory.createUnion(mapModel,ModelFactory.createUnion(tagModel,requestModel)));

        routeReasoner = createReasonerForInteraction(ROUTE_RULES);
        interestedPlacesReasoner = createReasonerForInteraction(INTERESTED_PLACES_RULES);
        busReasoner = createReasonerForInteraction(BUS_RULES);

        addPlaces();
        addInterests();
        addTagsToPlaces();
        calcAllDistances();
        addTagsSimilarities();
        addBusRoutes();
        calcNearestBusStations();
        ontologyModel.write(System.out);
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

    public Resource addPlace(String id, String name, String description, double latitude, double longitude)
    {
        Individual place = ontologyModel.createIndividual(ontologyModel.createResource());
        place.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.PLACE));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME), ontologyModel.createTypedLiteral(name));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_DESCRIPTION), ontologyModel.createTypedLiteral(description));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LATITUDE), ontologyModel.createTypedLiteral(latitude));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LONGITUDE), ontologyModel.createTypedLiteral(longitude));
        place.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_ID), ontologyModel.createTypedLiteral(id));
        places.put(id,place);
        return place;
    }

    public Resource addInterestTag(int id, String name)
    {
        Individual tag = ontologyModel.createIndividual(ontologyModel.createResource());
        tag.setOntClass(ontologyModel.getOntClass(OntologyClasses.Tag.INTEREST_TAG));
        tag.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Tag.HAS_ID), ontologyModel.createTypedLiteral(id));
        tag.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Tag.HAS_NAME),ontologyModel.createTypedLiteral(name));
        tags.put(id,tag);
        return tag;
    }

    public Resource addRequest(double speed, List<Resource> tags)
    {
        Individual request = ontologyModel.createIndividual(ontologyModel.createResource());
        request.setOntClass(ontologyModel.getOntClass(OntologyClasses.Request.REQUEST));
        request.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Request.HAS_AVG_SPEED), ontologyModel.createTypedLiteral(speed));
        tags.forEach(new Consumer<Resource>() {
            @Override
            public void accept(Resource resource) {
                request.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Tag.HAS_INTEREST_TAG),resource);
            }
        });

        return request;
    }

    public Resource addEvent(String id, String name, String description, List<String> placeIds)
    {
        Individual event = ontologyModel.createIndividual(ontologyModel.createResource());
        event.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.EVENT));
        event.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME), name);
        event.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_DESCRIPTION),description);
        event.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_ID),id);

        placeIds.forEach(s -> {
            Resource place = places.get(s);
            event.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.IN_PLACE),place);
        });

        return event;
    }

    public Resource addBusRoute(List<String> stations)
    {
        Individual r = ontologyModel.createIndividual(ontologyModel.createResource());
        r.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.BUS_ROUTE));

        List<Resource> busRouteNodes = new ArrayList<>();

        for (int i=0; i<stations.size(); i++)
        {
            Resource station = places.get(stations.get(i));
            Individual busRouteNode = ontologyModel.createIndividual(ontologyModel.createResource());
            busRouteNode.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.BUS_ROUTE_NODE));
            busRouteNode.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION),station);
            r.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_BUS_ROUTE_NODE),busRouteNode);
            busRouteNode.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.OF_ROUTE),r);
            busRouteNodes.add(busRouteNode);
        }
        r.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_FIRST_BUS_ROUTE_NODE),busRouteNodes.get(0));
        for (int i=1; i< busRouteNodes.size(); i++)
        {
            busRouteNodes.get(i-1).addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_NEXT_BUS_ROUTE_NODE),busRouteNodes.get(i));
        }

        return r;
    }

    public void addInterestTagToPlace(Resource place, Resource tag)
    {
        place.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Tag.HAS_INTEREST_TAG), tag);
    }

    public void addInterestTagToRequest(Resource request, Resource tag)
    {
        request.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Tag.HAS_INTEREST_TAG), tag);
    }

    public void addTagSimilarity(Resource tag1, Resource tag2, double similarity)
    {
        Individual s = ontologyModel.createIndividual(ontologyModel.createResource());
        s.setOntClass(ontologyModel.getOntClass(OntologyClasses.Tag.TAG_SIMILARITY));
        s.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Tag.HAS_SIMILARITY), ontologyModel.createTypedLiteral(similarity));
        s.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Tag.HAS_TAG1), tag1);
        s.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Tag.HAS_TAG2),tag2);
    }

    public void addDistance(Resource place1, Resource place2, double distance)
    {
        Individual dist = ontologyModel.createIndividual(ontologyModel.createResource());
        dist.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.DISTANCE));
        dist.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.STARTS_FROM), place1);
        dist.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.FINISHES_TO), place2);
        dist.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LENGTH), ontologyModel.createTypedLiteral(distance));
        dist.addProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LENGTH),ontologyModel.createTypedLiteral(dist));
    }

    public List<String> findInterestedPlaces(List<Integer> interestTagIds)
    {
        List<String> interestPlacesIds = new ArrayList<>();
        List<Resource> tagIds = new ArrayList<>();

        for (Integer tagId: interestTagIds)
        {
            tagIds.add(tags.get(tagId));
        }

        //tagIds.add(tags.get(10));
        //tagIds.add(tags.get(1));
        addRequest(2, tagIds);

        InfModel infModel = ModelFactory.createInfModel(interestedPlacesReasoner, ontologyModel);
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> "+
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/9/request#> "+
                "SELECT ?p ?id "+
                "WHERE { "+
                "?r ro:hasInterestingPlace ?p ."+
                " ?p mo:hasID ?id . "+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, infModel);
        ResultSet rs = qExec.execSelect();

        while (rs.hasNext())
        {
            //Resource place = rs.next().getResource("p");
            interestPlacesIds.add(rs.next().getLiteral("id").getString());
            //System.out.println("INTERESTED PLACE: "+place);
        }
        return interestPlacesIds;
    }

    public List<String> placeOrder(List<String> placesIds)
    {
        HashMap<String,HashMap<String,Double>> distances = new HashMap<>();
        List<String> order = new ArrayList<>();

        for (String placeId1: placesIds)
        {
            for (String placeId2: placesIds)
            {
                if (placeId1 != placeId2)
                {
                    Resource place1 = places.get(placeId1);
                    Resource place2 = places.get(placeId2);

                    double latitude1 = places.get(placeId1).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LATITUDE)).getDouble();
                    double longitude1 = places.get(placeId1).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LONGITUDE)).getDouble();

                    double latitude2 = places.get(placeId2).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LATITUDE)).getDouble();
                    double longitude2 = places.get(placeId2).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LONGITUDE)).getDouble();

                    if (!distances.containsKey(placeId1))
                    {
                        distances.put(placeId1,new HashMap<>());
                    }
                    double dist = Math.sqrt(Math.pow(latitude1-latitude2,2) + Math.pow(longitude1-longitude2,2));
                    distances.get(placeId1).put(placeId2,dist);
                }
            }
        }

        order.add(placesIds.get(0));
        for (int i=0; i<order.size(); i++)
        {
            String placeId1 = order.get(i);
            HashMap<String,Double> distTo = distances.get(placeId1);
            double min = -1;
            String nextPlace = "";
            for (String placeId2: distTo.keySet())
            {
                if (!order.contains(placeId2) && (min == -1 || min>distTo.get(placeId2)))
                {
                    min = distTo.get(placeId2);
                    nextPlace = placeId2;
                }
            }
            if (nextPlace!="")
                order.add(nextPlace);
        }

        return order;
    }

    public void calcNearestBusStations()
    {
        for (String id: places.keySet())
        {
            calcNearestBusStations(id);
        }
    }
    public void calcNearestBusStations(String id)
    {
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> "+
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/9/request#> "+
                "PREFIX f: <http://www.ontotext.com/sparql/functions/>"+

                "SELECT ?p ?p2  ?p3 ((( 0.56*0.56*(?lon-?lon2)*(?lon-?lon2)+(?lat-?lat2)*(?lat-?lat2))) as ?dist1) ((( 0.56*0.56*(?lon-?lon3)*(?lon-?lon3)+(?lat-?lat3)*(?lat-?lat3))) as ?dist2)"+
                "WHERE { "+
                " ?p a mo:Place ."+
                " ?p mo:hasID \""+id+"\" . "+
                " ?p mo:hasLongitude ?lon . "+
                " ?p mo:hasLatitude ?lat . "+

                " ?p2 a mo:BusStop ."+
                " ?p2 mo:hasID ?id2 . "+
                " ?p2 mo:hasLongitude ?lon2 . "+
                " ?p2 mo:hasLatitude ?lat2 . "+
                " ?brn1 mo:ofRoute ?r1 ."+
                "?brn1 mo:hasAccordingStation ?p2 ."+

                " ?p3 a mo:BusStop ."+
                " ?p3 mo:hasID ?id3 . "+
                " ?p3 mo:hasLongitude ?lon3 . "+
                " ?p3 mo:hasLatitude ?lat3 . "+
                " ?brn2 mo:ofRoute ?r2 ."+
                "?brn2 mo:hasAccordingStation ?p3 ."+

                "FILTER (?r1 != ?r2)"+

                "}"+
                "ORDER BY ASC(?dist) ASC(?dist2) LIMIT 1";

        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();

        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            Resource place = qs.getResource("p");
            Resource busStop = qs.getResource("p2");
            Resource busStop1 = qs.getResource("p3");
            place.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_NEAREST_BUS_STOP),busStop);
            place.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_NEAREST_BUS_STOP),busStop1);
            double dist1 = qs.getLiteral("dist1").getDouble();
            double dist2 = qs.getLiteral("dist2").getDouble();
            addDistance(place,busStop,Math.sqrt(dist1));
            addDistance(place,busStop1,Math.sqrt(dist2));
            //System.out.println("PLACE: "+ place.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getLiteral().getString() + "    STOP:" + busStop.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getLiteral().getString() + "    STOP:" + busStop1.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getLiteral().getString() );
        }
    }

    public void calcAllDistances()
    {
        places.keySet().forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                calcAllDistances(s);
            }
        });

        for (String placeId1 : distances.keySet())
        {
            for (String placeId2: distances.get(placeId1).keySet())
            {
                Resource place1 = places.get(placeId1);
                Resource place2 = places.get(placeId2);
                addDistance(place1,place2,distances.get(placeId1).get(placeId2));
            }
        }

        System.out.println(distances);
    }

    public void calcAllDistances(String fromId)
    {
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> "+
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/9/request#> "+
                "PREFIX f: <http://www.ontotext.com/sparql/functions/>"+
                "SELECT ?p ?p2  ?lon ?lat ?id2 ?lon2 ?lat2 (111300*abs(?lat2-?lat) as ?distLat) (abs(?lon-?lon2) as ?difflon)"+
                "WHERE { "+
                " ?p a mo:Place ."+
                " ?p mo:hasID \""+fromId+"\" . "+
                " ?p mo:hasLongitude ?lon . "+
                " ?p mo:hasLatitude ?lat . "+
                "{"+
                "SELECT ?p2 ?id2 ?lon2 ?lat2 WHERE {"+
                " ?p2 a mo:Place ."+
                " ?p2 mo:hasID ?id2 . "+
                " ?p2 mo:hasLongitude ?lon2 . "+
                " ?p2 mo:hasLatitude ?lat2 . "+
                "}"+
                "}"+
                "FILTER((\""+fromId+"\" != ?id2) && (?lat>0) && (?lat2>0)) ."+
                "}"+
                "ORDER BY ASC(0.56*0.56*(?lon-?lon2)*(?lon-?lon2)+(?lat-?lat2)*(?lat-?lat2)) LIMIT 4 ";
                //"ORDER BY ASC((?lon-?lon2)*(?lon-?lon2)+(?lat-?lat2)*(?lat-?lat2)) LIMIT 4";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();

        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            //System.out.println("Dist Lat: "+qs.get("distLat")+" "+qs.get("id")+" "+qs.get("lon")+" "+qs.get("lat")+"; "+qs.get("id2")+" "+qs.get("lon2")+" "+qs.get("lat2"));
            double lat = qs.get("lat").asLiteral().getDouble();
            double lat2 = qs.get("lat2").asLiteral().getDouble();
            double distLon = 6371000 * cos(3.14*(lat-lat2)/360) * (qs.get("difflon").asLiteral().getDouble()*3.14/180);
            //System.out.println("ID1: "+qs.get("id")+" ID2: "+qs.get("id2")+" DISTLAT: " + qs.get("distLat") + " DISTLON: "+distLon);
            //System.out.println("PLACE1: "+ qs.getResource("p").getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getLiteral().getString() + "    STOP:" + qs.getResource("p2").getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getLiteral().getString() );

            if (!distances.containsKey(fromId))
                distances.put(fromId, new HashMap<>());
            distances.get(fromId).put(qs.get("id2").asLiteral().getString(),distLon+qs.get("distLat").asLiteral().getDouble());

            //distances.put(new AbstractMap.SimpleEntry<>(qs.get("id").asLiteral().getString(), qs.get("id2").asLiteral().getString()) );
            //addDistance(place1,place2,qs.get("distLat").asLiteral().getDouble()+distLon);
        }

        /*for (String placeId1 : distances.keySet())
        {
            for (String placeId2: distances.get(placeId1).keySet())
            {
                Resource place1 = places.get(placeId1);
                Resource place2 = places.get(placeId2);
                addDistance(place1,place2,distances.get(placeId1).get(placeId2));
            }
        }*/
        //System.out.println(distances);
    }

    public List<List<Double>> getAllLines()
    {
        List<List<Double>> result = new ArrayList<>();
        for (String placeId1: distances.keySet())
        {
            for (String placeId2: distances.get(placeId1).keySet())
            {
                double latitude1 = places.get(placeId1).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LATITUDE)).getDouble();
                double longitude1 = places.get(placeId1).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LONGITUDE)).getDouble();

                double latitude2 = places.get(placeId2).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LATITUDE)).getDouble();
                double longitude2 = places.get(placeId2).getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_LONGITUDE)).getDouble();

                List<Double> coords = new ArrayList<>();
                coords.add(latitude1);
                coords.add(longitude1);
                coords.add(latitude2);
                coords.add(longitude2);
                result.add(coords);
            }
        }
        return result;
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

    public String getXMLText()
    {
        return ontologyModel.toString();
    }

    public void writeToFile() throws IOException {
        OntModel m = ontologyModel;
        RDFWriter writer = m.getWriter();
        m = null; // m is no longer needed.
        //writer.setErrorHandler(myErrorHandler);
        writer.setProperty("showXmlDeclaration","true");
        writer.setProperty("tab","8");
        writer.setProperty("relativeURIs","same-document,relative");
        OutputStream out = new FileOutputStream("foo.rdf");
        writer.write(ontologyModel, out, "http://example.org/");
        out.close();
    }
}
