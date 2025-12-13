package com.example.restaurantapp.models;

import com.google.firebase.Timestamp;

public class AttendanceModel {
    private String id;
    private String staffId;
    private String shift;
    private Timestamp checkIn;
    private Timestamp checkOut;

    public AttendanceModel() {}

    public AttendanceModel(String staffId, String shift, Timestamp checkIn, Timestamp checkOut) {
        this.staffId = staffId;
        this.shift = shift;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public Timestamp getCheckIn() { return checkIn; }
    public void setCheckIn(Timestamp checkIn) { this.checkIn = checkIn; }

    public Timestamp getCheckOut() { return checkOut; }
    public void setCheckOut(Timestamp checkOut) { this.checkOut = checkOut; }
}
