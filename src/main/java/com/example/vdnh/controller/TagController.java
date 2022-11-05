package com.example.vdnh.controller;

import io.swagger.models.auth.In;
import ontology.VDNHModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/tag")
public class TagController {

    @GetMapping("/interest/all")
    public HashMap<Integer,String> getAll()
    {
        return VDNHModel.getModel().getAllInterestTags();
    }

    @GetMapping("/map")
    public HashMap<String,HashMap<String,Double>> similarityMap()
    {
        return VDNHModel.getModel().getTagSimilarityHeatMap();
    }
}
