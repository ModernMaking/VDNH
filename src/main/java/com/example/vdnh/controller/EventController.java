package com.example.vdnh.controller;

import com.example.vdnh.model.EventTable;
import com.example.vdnh.model.EventPlace;
import com.example.vdnh.model.Place;
import com.example.vdnh.repo.EventPlaceRepository;
import com.example.vdnh.repo.EventRepository;
import com.example.vdnh.repo.PlaceRepository;
import com.google.gson.Gson;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.function.Consumer;

@RestController
@RequestMapping("/event")
public class EventController {
    @org.springframework.beans.factory.annotation.Autowired(required=true)
    EventRepository eventRepository;

    @org.springframework.beans.factory.annotation.Autowired(required=true)
    PlaceRepository placeRepository;

    @org.springframework.beans.factory.annotation.Autowired(required=true)
    EventPlaceRepository eventPlaceRepository;

    @RequestMapping("/addAll")
    public void addAll() throws FileNotFoundException, ParseException {
        JSONParser parser = new JSONParser( new FileInputStream(new File("src/main/resources/export.json")) );


        Object obj = parser.parse();//parseObject();//parse();//parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
        String jsonInString = new Gson().toJson(obj);
        JSONObject mJSONObject = new JSONObject(jsonInString);

        JSONObject events = mJSONObject.getJSONObject("events");
        for (String eventId: events.keySet()) {
            JSONObject event = events.getJSONObject(eventId);
            String previewtext = event.getString("preview_text");
            String title = event.getString("title");
            JSONArray eventPlaces = event.getJSONArray("places");

            EventTable eventTable1 = new EventTable(Long.parseLong(eventId), title,previewtext);
            eventRepository.save(eventTable1);

            eventPlaces.forEach(new Consumer<Object>() {
                @Override
                public void accept(Object o) {
                    Place place = placeRepository.findById((Long.parseLong(o.toString()))).get();
                    EventPlace eventPlace = new EventPlace(eventTable1,place);
                    eventPlaceRepository.save(eventPlace);
                }

                @Override
                public Consumer<Object> andThen(Consumer<? super Object> after) {
                    return Consumer.super.andThen(after);
                }
            });


        }
    }
}
