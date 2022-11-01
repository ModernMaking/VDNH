package com.example.vdnh.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;

@Table
@Entity
public class EventTable {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    private String title;
    @Size(max = 10000)
    private String description;

    public EventTable(Long id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public EventTable() {

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
