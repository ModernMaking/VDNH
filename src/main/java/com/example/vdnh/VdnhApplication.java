package com.example.vdnh;

import com.example.vdnh.controller.PlaceController;
import com.example.vdnh.model.Place;
import com.example.vdnh.repo.PlaceRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.persistence.Entity;

@SpringBootApplication(scanBasePackageClasses = {PlaceRepository.class})
@EntityScan(basePackageClasses = {Place.class})
@ComponentScan(basePackageClasses = {PlaceController.class})
@EnableJpaRepositories(basePackageClasses = {PlaceRepository.class})
@Configuration
public class VdnhApplication {

    public static void main(String[] args) {
        SpringApplication.run(VdnhApplication.class, args);
    }

}
