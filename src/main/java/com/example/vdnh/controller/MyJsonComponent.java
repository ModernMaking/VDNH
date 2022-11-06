package com.example.vdnh.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import ontology.VDNHModel;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class MyJsonComponent {

    public static class Serializer extends JsonSerializer<VDNHModel.RouteNode> {

        @Override
        public void serialize(VDNHModel.RouteNode routeNode, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("startDateTime", routeNode.getStartDateTime().toString());
            jsonGenerator.writeStringField("finishDateTime",routeNode.getPlaceIdFinish().toString());
            jsonGenerator.writeStringField("placeIdStart",routeNode.getPlaceIdStart());
            jsonGenerator.writeStringField("placeIdFinish",routeNode.getPlaceIdFinish());
            jsonGenerator.writeArrayFieldStart("otherPlaces");
            jsonGenerator.writeArray(routeNode.getOtherPlaces().toArray(new String[0]), 0, routeNode.getOtherPlaces().size());
            jsonGenerator.writeEndArray();
            jsonGenerator.writeStringField("durationMins",String.valueOf(routeNode.getDurationMins()));
            jsonGenerator.writeStringField("totalMins",String.valueOf(routeNode.getTotalMins()));
            jsonGenerator.writeStringField("description",routeNode.getDescription());
            jsonGenerator.writeEndObject();
        }
    }



}