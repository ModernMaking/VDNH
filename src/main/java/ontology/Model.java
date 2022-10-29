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

    org.apache.jena.rdf.model.Model mapModel = null;

    org.apache.jena.rdf.model.Model tagModel = null;

    org.apache.jena.rdf.model.Model requestModel = null;

    OntModel ontologyModel = null;

    Resource entrance = null;

    Reasoner routeReasoner = null;

    Reasoner interestedPlacesReasoner = null;

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
                if (object.has("children"))
                {
                    JSONArray children = object.getJSONArray("children");
                    //System.out.println(children);
                    addTagsFromJSONArray(children);
                }
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



            //Place place = new Place(Integer.valueOf(placeId).longValue(),title, type, latitude, longitude);
            //placeRepository.save(place);
        }
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
                    Integer tagId = (Integer) o;
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

        addPlaces();
        addInterests();
        addTagsToPlaces();
        calcAllDistances();

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

    public void addInterestTagToPlace(Resource place, Resource tag)
    {
        place.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Tag.HAS_INTEREST_TAG), tag);
    }

    public void addInterestTagToRequest(Resource request, Resource tag)
    {
        request.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Tag.HAS_INTEREST_TAG), tag);
    }

    public void addDistance(Resource place1, Resource place2, double distance)
    {
        Individual dist = ontologyModel.createIndividual(ontologyModel.createResource());
        dist.setOntClass(ontologyModel.getOntClass(OntologyClasses.Map.DISTANCE));
        dist.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.STARTS_FROM), place1);
        dist.addProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.FINISHES_TO), place2);
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

    public void calcAllDistances()
    {
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> "+
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/9/request#> "+
                "PREFIX f: <http://www.ontotext.com/sparql/functions/>"+
                "SELECT ?p ?p2 ?id ?lon ?lat ?id2 ?lon2 ?lat2 (111300*abs(?lat2-?lat) as ?distLat) (abs(?lon-?lon2) as ?difflon)"+
                "WHERE { "+
                " ?p a mo:Place ."+
                " ?p mo:hasID ?id . "+
                " ?p mo:hasLongitude ?lon . "+
                " ?p mo:hasLatitude ?lat . "+
                "{"+
                "SELECT ?p2 ?id2 ?lon2 ?lat2 WHERE {"+
                " ?p2 a mo:Place ."+
                " ?p2 mo:hasID ?id2 . "+
                " ?p2 mo:hasLongitude ?lon2 . "+
                " ?p2 mo:hasLatitude ?lat2 . "+
                "}"+
                "ORDER BY ASC((?lon-?lon2)*(?lon-?lon2)+(?lat-?lat2)*(?lat-?lat2)) LIMIT 4 "+
                "}"+
                "BIND (f:cos((3.14*(?lat2+?lat)/360)) AS ?cos) ."+
                "FILTER((?id != ?id2) && (?lat>0) && (?lat2>0)) ."+
                "}";
                //"ORDER BY ASC((?lon-?lon2)*(?lon-?lon2)+(?lat-?lat2)*(?lat-?lat2)) LIMIT 4";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();

        while (rs.hasNext())
        {
            QuerySolution qs = rs.next();
            System.out.println("Dist Lat: "+qs.get("distLat")+" "+qs.get("id")+" "+qs.get("lon")+" "+qs.get("lat")+"; "+qs.get("id2")+" "+qs.get("lon2")+" "+qs.get("lat2"));
            double lat = qs.get("lat").asLiteral().getDouble();
            double lat2 = qs.get("lat2").asLiteral().getDouble();
            double distLon = 6371000 * cos(3.14*(lat-lat2)/360) * (qs.get("difflon").asLiteral().getDouble()*3.14/180);
            System.out.println("ID1: "+qs.get("id")+" ID2: "+qs.get("id2")+" DISTLAT: " + qs.get("distLat") + " DISTLON: "+distLon);


            if (!distances.containsKey(qs.get("id").asLiteral().getString()))
                distances.put(qs.get("id").asLiteral().getString(), new HashMap<>());
            distances.get(qs.get("id").asLiteral().getString()).put(qs.get("id2").asLiteral().getString(),distLon+qs.get("distLat").asLiteral().getDouble());

            //distances.put(new AbstractMap.SimpleEntry<>(qs.get("id").asLiteral().getString(), qs.get("id2").asLiteral().getString()) );
            //addDistance(place1,place2,qs.get("distLat").asLiteral().getDouble()+distLon);
        }

        for (String placeId1 : distances.keySet())
        {
            for (String placeId2: distances.get(placeId1).keySet())
            {
                Resource place1 = places.get(placeId1);
                Resource place2 = places.get(placeId2);
                addDistance(place1,place2,distances.get(placeId1).get(placeId2));
            }
        }
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

            //System.out.println(interruptPoint+" "+name);
            while (interruptPoint.hasProperty(infModel.getProperty(ObjectProperties.Route.HAS_PREVIOUS)))
            {
                interruptPoint = interruptPoint.getPropertyResourceValue(infModel.getProperty(ObjectProperties.Route.HAS_PREVIOUS));
                point = interruptPoint.getPropertyResourceValue(infModel.getProperty(ObjectProperties.Route.HAS_ACCORDING_OBJECT));
                name = point.hasProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)) ? point.getProperty(infModel.getProperty(DataProperties.Map.HAS_NAME)).getString() : "";


                double time = interruptPoint.getProperty(infModel.getProperty(DataProperties.Route.HAS_TOTAL_TIME)).getDouble();

                //System.out.println(interruptPoint+" "+name+"    Время в пути: "+time+" минут");
            }

            //System.out.println("=================");
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
