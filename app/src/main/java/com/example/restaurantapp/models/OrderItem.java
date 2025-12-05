package com.example.restaurantapp.models;

public class OrderItem {
    private String menuId;
    private int quantity;

    public OrderItem() {}

    public OrderItem(String menuId, int quantity) {
        this.menuId = menuId;
        this.quantity = quantity;
    }

    public String getMenuId() { return menuId; }
    public void setMenuId(String menuId) { this.menuId = menuId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
