package com.example.restaurantapp.models;

public class ReservationModel {
    private String id;
    private String customerId;
    private String customerName;
    private String tableId;
    private String tableName;
    private String date; // yyyy-MM-dd
    private String time; // HH:mm
    private int guests;
    private String status; // pending, confirmed, cancelled, completed
    private String note;
    private long createdAt;

    public ReservationModel() {}

    public ReservationModel(String customerId, String customerName, String tableId, 
                           String tableName, String date, String time, int guests, String note) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.tableId = tableId;
        this.tableName = tableName;
        this.date = date;
        this.time = time;
        this.guests = guests;
        this.note = note;
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getStatusDisplay() {
        switch (status) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "cancelled": return "Đã hủy";
            case "completed": return "Hoàn thành";
            default: return status;
        }
    }
}
