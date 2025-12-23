package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class OrderModel {
    private String id;
    private String customerId;
    private String staffId;
    private String tableId;
    private String tableName;
    private String orderType; // dine_in, takeaway
    private String deliveryAddress;
    private String deliveryPhone;
    private List<OrderItem> items;
    private String status; // pending, confirmed, preparing, served, paid, cancelled
    private Double subtotal;
    private Double discount;
    private Double total;
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    
    public String getDeliveryPhone() { return deliveryPhone; }
    public void setDeliveryPhone(String deliveryPhone) { this.deliveryPhone = deliveryPhone; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public String getStatusDisplay() {
        if (status == null) return "Không xác định";
        switch (status) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "preparing": return "Đang chuẩn bị";
            case "served": return "Đã phục vụ";
            case "paid": return "Đã thanh toán";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }
}
