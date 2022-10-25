package com.example.vdnh.model;


import javax.persistence.*;

@Entity
@Table
public class Place {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    private String title;

    public Place(Long id, String title, String type, double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Place() {

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    private String type;

    private double latitude;

    private double longitude;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
