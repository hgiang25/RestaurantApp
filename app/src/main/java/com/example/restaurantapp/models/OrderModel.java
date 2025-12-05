package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class OrderModel {
    private String customerId;
    private String staffId; // có thể null nếu chưa được xử lý
    private String tableId;
    private List<OrderItem> items;
    private String status; // pending, preparing, served, paid
    private Timestamp createdAt;

    public OrderModel() {}

    public OrderModel(String customerId, String staffId, String tableId, List<OrderItem> items, String status, Timestamp createdAt) {
        this.customerId = customerId;
        this.staffId = staffId;
        this.tableId = tableId;
        this.items = items;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
