package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

public class PromotionModel {
    private String id;
    private String name;
    private String code;
    private double discountPercent;
    private double maxDiscount;
    private double minOrderAmount;
    private Timestamp validUntil;
    private boolean active;

    public PromotionModel() {}

    public PromotionModel(String name, String code, double discountPercent, double maxDiscount,
                          double minOrderAmount, Timestamp validUntil, boolean active) {
        this.name = name;
        this.code = code;
        this.discountPercent = discountPercent;
        this.maxDiscount = maxDiscount;
        this.minOrderAmount = minOrderAmount;
        this.validUntil = validUntil;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public double getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(double maxDiscount) { this.maxDiscount = maxDiscount; }

    public double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public Timestamp getValidUntil() { return validUntil; }
    public void setValidUntil(Timestamp validUntil) { this.validUntil = validUntil; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
