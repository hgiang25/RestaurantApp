package com.example.restaurantapp.models;

public class InventoryModel {
    private String id;
    private String name;
    private double quantity;
    private String unit;
    private double minQuantity; // Ngưỡng cảnh báo hết hàng

    public InventoryModel() {}

    public InventoryModel(String name, double quantity, String unit, double minQuantity) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.minQuantity = minQuantity;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getMinQuantity() { return minQuantity; }
    public void setMinQuantity(double minQuantity) { this.minQuantity = minQuantity; }

    public boolean isLowStock() {
        return quantity <= minQuantity;
    }
}
