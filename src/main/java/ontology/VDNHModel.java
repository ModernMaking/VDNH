package ontology;

import io.swagger.models.auth.In;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.boot.jackson.JsonComponent;

import java.io.InputStream;
import java.io.Serializable;
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

    /**
     * Возможно ли добраться на автобусе от места1 до места2
     * @param placeId1 Место 1 (ID)
     * @param placeId2 Место 2 (ID)
     * @return
     */
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

    /**
     * Найти автобусный маршрут между место1 и место2
     * @param placeId1
     * @param placeId2
     * @return
     */
    public RouteNode findBusRoute(String placeId1, String placeId2)
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

        List<String> internalBusStopIds = new ArrayList<>();

        int counter = 0;
        while (brn1 != brn2 && brn1 != null)
        {
            Resource busStop = brn1.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION));
            String name = busStop.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getString();
            String busStopId = busStop.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_ID)).getString();
            internalBusStopIds.add(busStopId);
            result.put("point"+(++counter),name);
            brn1 = brn1.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_NEXT_BUS_ROUTE_NODE));

            if (brn1.getProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION)).getResource().getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_ID)).getString()
                    == brn2.getProperty(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION)).getResource().getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_ID)).getString())
                break;
        }

        RouteNode routeNode = new RouteNode(LocalDateTime.now(),LocalDateTime.now(),placeId1,placeId2,internalBusStopIds,0,0,"");

        return routeNode;
    }

    /**
     * Найти ближайшие места от остановки автобуса
     * @param stationId
     * @return
     */
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

    /**
     * Список всех маршрутов автобусов
     * @return
     */
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

    /**
     * Автобусная остановка для точки маршрута
     * @param brn
     * @return
     */
    private String getBusStationIdForBRN(Resource brn)
    {
        Resource station = brn.getPropertyResourceValue(ontologyModel.getObjectProperty(ObjectProperties.Map.HAS_ACCORDING_STATION));
        return station.getProperty(ontologyModel.getDatatypeProperty((DataProperties.Map.HAS_ID))).getString();
    }

    /**
     * Найти все станции, до которых можно доехать со станции ...
     * @param id ИД станции
     * @return
     */
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

    /**
     * Список всех тегов, связанных с интересами
     * @return
     */
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

    /**
     * Места по тегам
     * @param tagIds ИД тегов (список)
     * @return
     */
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

    /**
     * Найти места, похожие на ...
     * @param placeId ИД места
     * @return
     */
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

    /**
     * Тепловая карта схожести тегов
     * @return
     */
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

    /**
     * Найти пешеходный маршрут между двумя местами
     * @param placeId1 место1
     * @param placeId2 место2
     * @param startDateTime Дата/время начала пути
     * @return
     */
    public List<VDNHModel.RouteNode> findRouteAsPlaceIdsBetweenPlaces(String placeId1, String placeId2, LocalDateTime startDateTime)
    {
        List<List<List<RouteNode>>> searchNodes = new ArrayList<>();
       List<List<List<String>>> search = new ArrayList<>();

       List<List<String>> firstLevel = new ArrayList<>();
       List<String> simplePath = new ArrayList<>();
       simplePath.add(placeId1);
       firstLevel.add(simplePath);
       search.add(firstLevel);

        List<List<RouteNode>> firstLevelNodes = new ArrayList<>();
        List<RouteNode> nodes = new ArrayList<>();

        Resource place1 = findPlaceById(placeId1);
        Resource place2 = findPlaceById(placeId2);

        String name1 = place1.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getString();
        String name2 = place2.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getString();

        RouteNode routeNode = new RouteNode(startDateTime,startDateTime,placeId1,placeId2,null,0,0,"Вы пройдете мимо "+name1+" ; "+name2);
        nodes.add(routeNode);
        firstLevelNodes.add(nodes);
        searchNodes.add(firstLevelNodes);

        Set<String> visited = new HashSet<>();
        visited.add(placeId1);

       int level = 0;
       while (level<300)
       {
           level++;
           List<List<String>> currLevel = new ArrayList<>();
           search.add(currLevel);
           List<List<String>> prevLevel = search.get(level-1);

           List<List<RouteNode>> currLevelNodes = new ArrayList<>();
           searchNodes.add(currLevelNodes);

           List<List<RouteNode>> prevLevelNodes = searchNodes.get(level-1);

           int pathIndex = 0;

           for (List<String> path: prevLevel)
           {
               List<RouteNode> nodePath = prevLevelNodes.get(pathIndex);
               String lastPoint = path.get(path.size()-1);
               String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                       "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                       "PREFIX to: <http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#> "+
                       "SELECT ?id2 ?len ?lat ?lon ?lat2 ?lon2 ?name ?name2 " +
                       "WHERE { " +
                       "?place a mo:Place . "+
                       "?place mo:hasID \""+lastPoint+"\" . "+
                       "?place mo:hasLatitude ?lat . "+
                       "?place mo:hasLongitude ?lon . "+
                       "?place mo:hasName ?name . "+
                       "?dist a mo:Distance ."+
                       "?dist mo:startsFrom ?place ."+
                       "?dist mo:finishesTo ?place2 . "+
                       "?place2 mo:hasID ?id2 . "+
                       "?place2 mo:hasLatitude ?lat2 . "+
                       "?place2 mo:hasLongitude ?lon2 . "+
                       "?place2 mo:hasName ?name2 . "+
                       "?dist mo:hasLength ?len ."+

                       "}";
               Query query = QueryFactory.create(queryString);
               QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
               ResultSet rs = qExec.execSelect();
               while (rs.hasNext())
               {
                   QuerySolution qs = rs.next();
                   String id2 = qs.getLiteral("id2").getString();
                   Double dist = qs.getLiteral("len").getDouble();
                   double latitude1 = qs.getLiteral("lat").getDouble();
                   double latitude2 = qs.getLiteral("lat2").getDouble();
                   double longitude1 = qs.getLiteral("lon").getDouble();
                   double longitude2 = qs.getLiteral("lon2").getDouble();
                   String pName1 = qs.getLiteral("name").getString();
                   String pName2 = qs.getLiteral("name2").getString();
                   List<String> path2 = new ArrayList<>(path);//.subList(0, path.size()-1);
                    List<RouteNode> nodePath2 = new ArrayList<>(nodePath);

                   if (!visited.contains(id2) && !path2.contains(id2))
                   {
                       visited.add(id2);
                       path2.add(id2);
                       currLevel.add(path2);

                       double time = dist/50;
                       int lastTotalMins = nodePath.get(nodePath2.size()-1).getTotalMins();
                        LocalDateTime lastFinishDateTime = nodePath2.get(nodePath2.size()-1).getFinishDateTime();
                       RouteNode routeNode1 = new RouteNode(lastFinishDateTime,lastFinishDateTime.plusSeconds((long) (time*60)),path2.get(path2.size()-2), path2.get(path2.size()-1), null, (int)time, (int)(lastTotalMins+(int)time), "Вы пройдете мимо: "+pName1+" ; "+pName2,latitude1,latitude2,longitude1,longitude2);
                       nodePath2.add(routeNode1);
                        currLevelNodes.add(nodePath2);
                       if (id2.equals(placeId2))
                           return nodePath2;//path2;
                   }

               }
               pathIndex++;
           }

       }

        return new ArrayList<>();
    }

    /**
     * Порядок посещения мест
     * @param placesIds
     * @return
     */
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
                    String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                            "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                            "PREFIX to: <http://www.semanticweb.org/dns/ontologies/2022/9/tag-ontology#> "+
                            "SELECT DISTINCT ?lon1 ?lon2 ?lat1 ?lat2 " +
                            "WHERE { " +
                            "?place1 a mo:Place . "+
                            "?place1 mo:hasID \""+placeId1+"\" . "+
                            "?place2 a mo:Place . "+
                            "?place2 mo:hasID \""+placeId2+"\" . "+
                            "?place1 mo:hasLatitude ?lat1 ."+
                            "?place1 mo:hasLongitude ?lon1 ."+
                            "?place2 mo:hasLatitude ?lat2 ."+
                            "?place2 mo:hasLongitude ?lon2 ."+
                            "}";
                    Query query = QueryFactory.create(queryString);
                    QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
                    ResultSet rs = qExec.execSelect();
                    QuerySolution qs = rs.next();
                    double longitude1 = qs.getLiteral("lon1").getDouble();
                    double longitude2 = qs.getLiteral("lon2").getDouble();
                    double latitude1 = qs.getLiteral("lat1").getDouble();
                    double latitude2 = qs.getLiteral("lat2").getDouble();

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

    /*public List<RouteNode> getRouteTrack(List<String> placeIds, LocalDateTime startDateTime, double speed)
    {

    }*/

    /**
     * Построить маршрут с учетом интересов и временных ограничений
     * @param tagIds ИД тегов (список)
     * @param startDateTime время/дата начала пути
     * @param finishDateTime время/дата конца пути
     * @return
     */
    public List<RouteNode> getRouteByTagsAndTimeLimit(List<Integer> tagIds, LocalDateTime startDateTime, LocalDateTime finishDateTime)
    {
        List<RouteNode> routeNodeList = new ArrayList<>();
        List<String> interestingPlacesOrder = placeOrder(placeIdsByTags(tagIds));
        LocalDateTime currDateTime = startDateTime;
        String prevPlaceId = "";
        for (String placeId : interestingPlacesOrder)
        {
            if (!prevPlaceId.equals(""))
            {
                List<RouteNode> walk = findRouteAsPlaceIdsBetweenPlaces(prevPlaceId,placeId,currDateTime);
                routeNodeList.addAll(walk);
                currDateTime = walk.get(walk.size()-1).getFinishDateTime();
            }
            if (false)
                continue;
            LocalDateTime finishTime = currDateTime.plusMinutes(30);

            Resource placeResource = findPlaceById(placeId);
            String name = placeResource.getProperty(ontologyModel.getDatatypeProperty(DataProperties.Map.HAS_NAME)).getString();

            RouteNode rn = new RouteNode(currDateTime,finishTime,placeId,placeId,null,30,0,"Остановка с заходом в павильон "+name+" ID:"+placeId);
            routeNodeList.add(rn);
            prevPlaceId = placeId;

            currDateTime = finishTime;
            if (currDateTime.isAfter(finishDateTime.minusMinutes(20)))
                break;

        }

        return routeNodeList;
    }

    private Resource findPlaceById(String placeId)
    {
        String queryString = "PREFIX mo: <http://www.semanticweb.org/dns/ontologies/2022/8/map-ontology#> " +
                "PREFIX ro: <http://www.semanticweb.org/dns/ontologies/2022/8/route#> " +
                "SELECT ?p " +
                "WHERE { " +
                "?p a mo:Place ."+
                "?p mo:hasID \""+placeId+"\" ."+
                "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qExec = QueryExecutionFactory.create(query, ontologyModel);
        ResultSet rs = qExec.execSelect();
        QuerySolution qs = rs.next();
        return qs.getResource("p");
    }

    /**
     * Класс, означающий точку маршрута (стоянка в павильоне, передвижение...)
     */
    public class RouteNode implements Serializable
    {
        public LocalDateTime getStartDateTime() {
            return startDateTime;
        }

        public LocalDateTime getFinishDateTime() {
            return finishDateTime;
        }

        public String getPlaceIdStart() {
            return placeIdStart;
        }

        public String getPlaceIdFinish() {
            return placeIdFinish;
        }

        public List<String> getOtherPlaces() {
            return otherPlaces;
        }

        public int getDurationMins() {
            return durationMins;
        }

        public int getTotalMins() {
            return totalMins;
        }

        public String getDescription() {
            return description;
        }

        private LocalDateTime startDateTime;
        private LocalDateTime finishDateTime;
        private String placeIdStart;
        private String placeIdFinish;
        private List<String> otherPlaces;
        private int durationMins;
        private int totalMins;
        private String description;
        private double latitude1;
        private double latitude2;
        private double longitude1;
        private double longitude2;

        public double getLatitude1() {
            return latitude1;
        }

        public double getLatitude2() {
            return latitude2;
        }

        public double getLongitude1() {
            return longitude1;
        }

        public double getLongitude2() {
            return longitude2;
        }

        public RouteNode(LocalDateTime startDateTime, LocalDateTime finishDateTime, String placeIdStart, String placeIdFinish, List<String> otherPlaces, int durationMins, int totalMins, String description, double latitude1, double latitude2, double longitude1, double longitude2) {
            this.startDateTime = startDateTime;
            this.finishDateTime = finishDateTime;
            this.placeIdStart = placeIdStart;
            this.placeIdFinish = placeIdFinish;
            this.otherPlaces = otherPlaces;
            this.durationMins = durationMins;
            this.totalMins = totalMins;
            this.description = description;
            this.latitude1 = latitude1;
            this.latitude2 = latitude2;
            this.longitude1 = longitude1;
            this.longitude2 = longitude2;
        }

        public RouteNode(LocalDateTime startDateTime, LocalDateTime finishDateTime, String placeIdStart, String placeIdFinish, List<String> otherPlaces, int durationMins, int totalMins, String description) {
            this.startDateTime = startDateTime;
            this.finishDateTime = finishDateTime;
            this.placeIdStart = placeIdStart;
            this.placeIdFinish = placeIdFinish;
            this.otherPlaces = otherPlaces;
            this.durationMins = durationMins;
            this.totalMins = totalMins;
            this.description = description;
        }
    }


    /**
     * Прочитать модель из RDF-файла
     * @param modelFile
     * @return
     */
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
