package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

public class TableModel {

    public static final String STATUS_FREE = "free";
    public static final String STATUS_OCCUPIED = "occupied";
    public static final String STATUS_RESERVED = "reserved";

    private String id;
    private String name;
    private int capacity;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public TableModel() {}

    public TableModel(String name, int capacity, String status) {
        this.name = name;
        this.capacity = capacity;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setStatus(String status) { this.status = status; }
}
