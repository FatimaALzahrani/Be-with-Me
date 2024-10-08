package com.waqf.bewithme;

public class Guide {
    private String name;
    private String phoneNumber;
    private double latitude;
    private double longitude;

    public Guide() {
        // Default constructor required for calls to DataSnapshot.getValue(Guide.class)
    }

    public Guide(String name, String phoneNumber, double latitude, double longitude) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
