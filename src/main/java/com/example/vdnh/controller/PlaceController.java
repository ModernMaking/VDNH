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
import org.apache.jena.base.Sys;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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



        JSONParser parser = new JSONParser(new FileReader(getClass().getResource("/").getPath() + "../classes/export.json"));


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

        //JSONObject jsonObject =  new JSONObject(obj);//(JSONObject) obj;

        //System.out.println(jsonObject);

            /*
            jsonObject.forEach(new BiConsumer<String, Object>() {
                @Override
                public void accept(String s, Object o) {
                    System.out.println(s);
                }

                @Override
                public BiConsumer<String, Object> andThen(BiConsumer<? super String, ? super Object> after) {
                    return BiConsumer.super.andThen(after);
                }
            });

            */

        //JSONObject visitors = (JSONObject) jsonObject.get("visitors");
        //System.out.println(visitors.get("sections"));

            /*JSONObject places = new JSONObject(jsonObject.get("places"));
            for (String key:
                 places.keySet()) {
                System.out.println(key);
            }*/

        //System.out.println(places);

            /*

            places.forEach(new BiConsumer<String, Object>() {
                @Override
                public void accept(String s, Object o) {
                    System.out.println(s);
                    //System.out.println(o);
                    JSONObject object = (JSONObject) o;

                    if (object.containsKey("coordinates"))
                    {
                        JSONArray coordinates = (JSONArray) object.get("coordinates");
                        System.out.println("longitude: "+coordinates.get(0).toString());
                        System.out.println("latitude: "+coordinates.get(1).toString());
                    }

                    if (object.containsKey("title"))
                    {
                        System.out.println("title: "+object.get("title"));
                    }

                    if (object.containsKey("type"))
                    {
                        System.out.println("type: "+object.get("type"));
                    }

                }

                @Override
                public BiConsumer<String, Object> andThen(BiConsumer<? super String, ? super Object> after) {
                    return BiConsumer.super.andThen(after);
                }
            });

             */

        //System.out.println(obj);
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

        return Model.getModel().findInterestedPlaces(tagIds);
    }

    //!!! Получить погоду на заданный день
    public HashMap<String,String> getWeather(Date date)
    {
        return new HashMap<>();
    }

}
