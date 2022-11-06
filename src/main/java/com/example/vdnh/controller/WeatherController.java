package com.example.vdnh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    @GetMapping("/get")
    public String getWeather()
    {
        final String uri = "https://api.gismeteo.net/v2/weather/current/4368/";
        RestTemplate restTemplate = new RestTemplate();
        //restTemplate.headForHeaders("X-Gismeteo-Token","56b30cb255.3443075");
        HashMap<String,String> headers = new HashMap<>();
        headers.put("X-Gismeteo-Token","56b30cb255.3443075");

        String result = restTemplate.getForObject(uri, String.class,headers);
        return result;
    }
}
