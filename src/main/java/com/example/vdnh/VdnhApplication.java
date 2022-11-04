package com.example.vdnh;

import com.example.vdnh.controller.EventController;
import com.example.vdnh.controller.PlaceController;
import com.example.vdnh.model.EventPlace;
import com.example.vdnh.model.EventTable;
import com.example.vdnh.model.Place;
import com.example.vdnh.repo.EventPlaceRepository;
import com.example.vdnh.repo.EventRepository;
import com.example.vdnh.repo.PlaceRepository;
import ontology.Model;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

@SpringBootApplication(scanBasePackageClasses = {PlaceRepository.class})
@EntityScan(basePackageClasses = {Place.class, EventTable.class, EventPlace.class})
@ComponentScan(basePackageClasses = {PlaceController.class, EventController.class})
@EnableJpaRepositories(basePackageClasses = {PlaceRepository.class, EventRepository.class, EventPlaceRepository.class})
@Configuration
public class VdnhApplication {

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
       /*try {
            Model.getModel().writeToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        SpringApplication.run(VdnhApplication.class, args);
    }

}
