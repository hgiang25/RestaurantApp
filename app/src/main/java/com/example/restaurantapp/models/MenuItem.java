package com.example.restaurantapp.models;

public class MenuItem {
    private String id;
    private String name;
    private double price;
    private String category;
    private boolean available;
    private String imageUrl;

    public MenuItem() {}

    public MenuItem(String name, double price, String category, boolean available) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.available = available;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
