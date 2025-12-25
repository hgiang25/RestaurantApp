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
    private String imageUrl; // Thêm trường này
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
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
