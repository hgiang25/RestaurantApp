package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

public class NotificationModel {
    private String id;
    private String userId;
    private String message;
    private Timestamp createdAt;

    public NotificationModel() {}

    public NotificationModel(String userId, String message, Timestamp createdAt) {
        this.userId = userId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
