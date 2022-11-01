package com.example.vdnh.model;

import javax.persistence.*;

@Table
@Entity
public class EventPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private EventTable eventTable;
    @ManyToOne
    @JoinColumn(name = "place_id")
    private Place place;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventPlace()
    {

    }

    public EventPlace(EventTable eventTable, Place place) {
        this.eventTable = eventTable;
        this.place = place;
    }

    public EventTable getEvent() {
        return eventTable;
    }

    public void setEvent(EventTable eventTable) {
        this.eventTable = eventTable;
    }

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }
}
