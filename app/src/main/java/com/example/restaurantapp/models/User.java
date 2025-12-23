package com.example.restaurantapp.models;

public class User {
    private String id;
    private String email;
    private String username;
    private String role;
    private int loyaltyPoints; // Điểm tích lũy

    public User() {}

    public User(String email, String username, String role) {
        this.email = email;
        this.username = username;
        this.role = role;
        this.loyaltyPoints = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    
    // Tính điểm tích lũy từ hóa đơn (1,000,000đ = 100 điểm => 10,000đ = 1 điểm)
    public static int calculatePointsFromBill(double billAmount) {
        return (int) (billAmount / 10000);
    }
    
    // Cộng điểm tích lũy từ hóa đơn
    public void addPointsFromBill(double billAmount) {
        this.loyaltyPoints += calculatePointsFromBill(billAmount);
    }
    
    // Cộng điểm tích lũy
    public void addLoyaltyPoints(int points) { this.loyaltyPoints += points; }
    
    // Trừ điểm tích lũy (khi đổi điểm)
    public boolean useLoyaltyPoints(int points) {
        if (this.loyaltyPoints >= points) {
            this.loyaltyPoints -= points;
            return true;
        }
        return false;
    }
}
