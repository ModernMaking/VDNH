package com.example.vdnh.controller;

import com.example.vdnh.model.Place;
import com.example.vdnh.repo.PlaceRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.jsonldjava.utils.Obj;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.models.auth.In;
import ontology.Model;
import ontology.VDNHModel;
import org.apache.jena.base.Sys;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;

import java.beans.Customizer;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RestController
@RequestMapping("/place")
public class PlaceController {

    @org.springframework.beans.factory.annotation.Autowired(required=true)
    PlaceRepository placeRepository;

    public static String parseUrl(URL url) {
        if (url == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        // открываем соедиение к указанному URL
        // помощью конструкции try-with-resources
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {

            String inputLine;
            // построчно считываем результат в объект StringBuilder
            while ((inputLine = in.readLine()) != null) {
                stringBuilder.append(inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    @GetMapping("/addAll")
    public void addAll() throws FileNotFoundException, MalformedURLException, ParseException {

        System.out.println("GET PATH: "+getClass().getResource("/").getPath());

        JSONParser parser = new JSONParser( new FileInputStream(new File("src/main/resources/export.json")) );//new JSONParser(new FileReader(getClass().getResource("/").getPath() + "../classes/export.json"));




        Object obj = parser.parse();//parseObject();//parse();//parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
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

            System.out.println("ID: "+placeId);
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

            Place place = new Place(Integer.valueOf(placeId).longValue(),title, type, latitude, longitude);
            placeRepository.save(place);
        }

        JSONObject events = mJSONObject.getJSONObject("events");
        for (String eventId: events.keySet()) {
            JSONObject event = events.getJSONObject(eventId);
            String previewtext = event.getString("preview_text");
            String title = event.getString("title");
            JSONArray eventPlaces = event.getJSONArray("places");
            //System.out.println(event);
        }


    }

    @GetMapping("/toiletNear")
    public List<Object> getToiletsNear(double latitude, double longitude)
    {
        return placeRepository.getToiletsNear(latitude,longitude);
    }

    @GetMapping("/busStationNear")
    public List<Object> getBusStationsNear(double latitude, double longitude)
    {
        return placeRepository.getBusStationsNear(latitude,longitude);
    }

    @GetMapping("/allStations")
    public List<Place> getAllBusStations()
    {
        return placeRepository.findAllByType("Остановка");
    }


    //!!! Метод для получения всех меток для карты
    @GetMapping("/all")
    public List<Place> getAllPlaces()
    {
        return placeRepository.findAllByLatitudeAfterAndLongitudeAfter(0,0);
    }

    @GetMapping("/all/withoutService")
    public List<Place> getAllPlacesExceptService()
    {
        List<String> exceptedTypes = new ArrayList<>();
        exceptedTypes.add("Туалеты");
        exceptedTypes.add("Остановка");
        exceptedTypes.add("Въезд");
        exceptedTypes.add("Парковка");
        exceptedTypes.add("Вход");
        exceptedTypes.add("Такси");
        exceptedTypes.add("Инфоцентр");
        exceptedTypes.add("Билеты");
        exceptedTypes.add("Прокат");
        exceptedTypes.add("Еда");
        exceptedTypes.add("Банкомат");
        exceptedTypes.add("Вендинговый аппарат");
        exceptedTypes.add("Читальня");
        return placeRepository.findAllByTypeNotIn(exceptedTypes);
    }

    @GetMapping("/allLines")
    public List<List<Double>> getAllLines()
    {
        return Model.getModel().getAllLines();
    }

    @GetMapping("/byTags")
    public List<String> getPlaceIdsByTags(@RequestBody String requestParams)
    {
        System.out.println(requestParams);


        Gson gson = new Gson();

        JsonArray arr = gson.fromJson(requestParams,JsonObject.class).get("tagIds").getAsJsonArray();
        List<Integer> tagIds = new ArrayList<>();

        arr.forEach(new Consumer<JsonElement>() {
            @Override
            public void accept(JsonElement jsonElement) {
                tagIds.add(jsonElement.getAsInt());
            }
        });

        return VDNHModel.getModel().placeIdsByTags(tagIds); // findInterestedPlaces(tagIds);
    }

    @PostMapping("/calcRoute")
    public String calcRouteNode(@RequestBody String requestParams)
    {
        System.out.println(requestParams);
        Gson gson = new Gson();
        JsonArray arr = gson.fromJson(requestParams,JsonObject.class).get("tagIds").getAsJsonArray();
        List<Integer> tagIds = new ArrayList<>();

        arr.forEach(new Consumer<JsonElement>() {
            @Override
            public void accept(JsonElement jsonElement) {
                tagIds.add(jsonElement.getAsInt());
            }
        });
        String start = gson.fromJson(requestParams,JsonObject.class).get("start").getAsString();
        String finish = gson.fromJson(requestParams,JsonObject.class).get("finish").getAsString();
        LocalDateTime startDateTime = LocalDateTime.parse(start);
        LocalDateTime finishDateTime = LocalDateTime.parse(finish);
        List<VDNHModel.RouteNode> routeNodeList = VDNHModel.getModel().getRouteByTagsAndTimeLimit(tagIds,startDateTime,finishDateTime);

        List<JSONObject> jsonObjectList = new ArrayList<>();
        routeNodeList.forEach(new Consumer<VDNHModel.RouteNode>() {
            @Override
            public void accept(VDNHModel.RouteNode routeNode) {
                jsonObjectList.add(new JSONObject(routeNode));
            }
        });
        //JsonArray jsonObject = new JSONArray(routeNodeList);
        return jsonObjectList.toString();
    }

    @PostMapping("/order/byTags")
    public List<Place> getPlaceIdsOrderByTags(@RequestBody String requestParams)
    {
        System.out.println(requestParams);
        Gson gson = new Gson();
        JsonArray arr = gson.fromJson(requestParams,JsonObject.class).get("tagIds").getAsJsonArray();
        List<Integer> tagIds = new ArrayList<>();

        arr.forEach(new Consumer<JsonElement>() {
            @Override
            public void accept(JsonElement jsonElement) {
                tagIds.add(jsonElement.getAsInt());
            }
        });

        List<String> placeIds = VDNHModel.getModel().placeOrder(VDNHModel.getModel().placeIdsByTags(tagIds));
        List<Place> places = new ArrayList<>();
        for (String placeId: placeIds) {
            places.add(placeRepository.findById(Long.parseLong(placeId)).get());
        }
        return places;
    }

    @GetMapping("/nearBusStation")
    public List<Place> getPlacesForBusStation(String stationId)
    {
        List<String> placeIds = VDNHModel.getModel().findNearestPlacesToStation(stationId);
        List<Place> places = new ArrayList<>();
        placeIds.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                Place p = placeRepository.findById(Long.parseLong(s)).get();
                places.add(p);
            }
        });
        return places;
    }

    @GetMapping("/allRoutes")
    public List<List<Place>> getAllBusRoutes()
    {

        List<List<String>> routes = VDNHModel.getModel().findAllBusRoutes();
        List<List<Place>> res = new ArrayList<>();
        for (int i=0; i<routes.size(); i++)
        {
            List<Place> currRoute = new ArrayList<>();
            routes.get(i).forEach(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    currRoute.add(placeRepository.findById(Long.parseLong(s)).get());
                }
            });

            res.add(currRoute);
        }
        return res;
    }

    @GetMapping("/busRouteBetweenPlaces")
    public String getBusRouteBetweenPlaces(String placeId1, String placeId2)
    {
        VDNHModel.RouteNode route = VDNHModel.getModel().findBusRoute(placeId1,placeId2);
        JSONObject jsonObject = new JSONObject(route);
        return jsonObject.toString();
    }

    @GetMapping("/reachable")
    public List<Place> getReachableStations(String id)
    {
        List<Place> places = new ArrayList<>();
        List<String> ids = VDNHModel.getModel().getAllReachableStationsFrom(id);
        ids.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                places.add(placeRepository.findById(Long.parseLong(s)).get());
            }
        });
        return places;
    }

    @GetMapping("/walkPath")
    public String getPathFromTo(String from, String to)
    {
        List<Place> places = new ArrayList<>();
        List<VDNHModel.RouteNode> ids = VDNHModel.getModel().findRouteAsPlaceIdsBetweenPlaces(from,to,LocalDateTime.now());
        /*ids.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                places.add(placeRepository.findById(Long.parseLong(s)).get());
            }
        });*/

        JSONArray jsonArray = new JSONArray(ids);
        String jsonArrayString = jsonArray.toString();
        return jsonArrayString;
    }

    @GetMapping("/similarTo")
    public List<Place> getPlacesSimilarTo(String placeId)
    {
        List<Place> places = new ArrayList<>();
        List<String> ids = VDNHModel.getModel().findPlacesSimilarTo(placeId);
        ids.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                places.add(placeRepository.findById(Long.parseLong(s)).get());
            }
        });
        return places;
    }

    //!!! Получить погоду на заданный день
    public HashMap<String,String> getWeather(Date date)
    {
        return new HashMap<>();
    }

}
