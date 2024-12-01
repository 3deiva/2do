package com.example.a2do;

public class Task {
    private String name;
    private String timestamp;
    private double latitude;
    private double longitude;

    public Task() {
        // Empty constructor for Firebase
    }

    public Task(String name, String timestamp, double latitude, double longitude) {
        this.name = name;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

