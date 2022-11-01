package com.example.vdnh;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import ontology.Model;
import org.apache.jena.atlas.json.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.annotation.Testable;

import java.beans.Customizer;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Testable
public class ModelTests {

    @Test
    public void test1()
    {
        Model m = Model.getModel();
        m.calcRoute();
    }

    @Test
    public void test3()
    {
        Model m = Model.getModel();
        List<Integer> tagIds = new ArrayList<>();
        tagIds.add(10);
        tagIds.add(1);
        List<String> placeIds = m.findInterestedPlaces(tagIds);
        System.out.println(placeIds);
    }

    @Test
    public void test2()
    {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader("C:\\Users\\DNS\\IdeaProjects\\VDNH\\src\\main\\resources\\export.json"));
            JSONObject jsonObject =  (JSONObject) obj;
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

            JSONObject visitors = (JSONObject) jsonObject.get("visitors");
            //System.out.println(visitors.get("sections"));

            JSONObject places = (JSONObject) jsonObject.get("places");
            //System.out.println(places);


            places.forEach(new BiConsumer<String, Object>() {
                @Override
                public void accept(String s, Object o) {
                    System.out.println(s);
                    System.out.println(o);
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

            JSONArray routes = (JSONArray) jsonObject.get("routes");
            routes.forEach(new Consumer<Object>() {
                @Override
                public void accept(Object o) {
                    System.out.println("ROUTE: "+o);
                }

                @Override
                public Consumer<Object> andThen(Consumer<? super Object> after) {
                    return Consumer.super.andThen(after);
                }
            });

            //System.out.println(obj);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void test4()
    {
        Model m = Model.getModel();
        System.out.println(m.hasBusRouteBetweenPlaces("246","294"));
        System.out.println(m.hasBusRouteBetweenPlaces("361","362"));
        System.out.println(m.findBusRoute("316","294"));
    }
}
