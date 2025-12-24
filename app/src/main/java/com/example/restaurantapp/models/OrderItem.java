package com.example.restaurantapp.models;

public class OrderItem {
    private String menuId;
    private String menuItemId; // Dùng cho công thức trừ kho
    private String name;
    private double price;
    private int quantity;

    public OrderItem() {}

    public OrderItem(String menuId, int quantity) {
        this.menuId = menuId;
        this.menuItemId = menuId;
        this.quantity = quantity;
    }

    public OrderItem(String menuId, String name, double price, int quantity) {
        this.menuId = menuId;
        this.menuItemId = menuId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getMenuId() { return menuId; }
    public void setMenuId(String menuId) { this.menuId = menuId; }

    public String getMenuItemId() { return menuItemId != null ? menuItemId : menuId; }
    public void setMenuItemId(String menuItemId) { this.menuItemId = menuItemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
