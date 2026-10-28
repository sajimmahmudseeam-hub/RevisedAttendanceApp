package com.example.revisedattendanceapp;

import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;

public class AttendanceRecord {
    private final StringProperty id;
    private final StringProperty name;
    private final StringProperty course;
    private final DoubleProperty overallRate;
    private final Map<String, String> dailyMap;
    private final Map<String, String> dailyReasonMap;

    public AttendanceRecord(String id, String name, String course) {
        this(id, name, course, 100.0);
    }

    public AttendanceRecord(String id, String name, String course, double overallRate) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.course = new SimpleStringProperty(course);
        this.overallRate = new SimpleDoubleProperty(overallRate);
        this.dailyMap = new HashMap<>();
        this.dailyReasonMap = new HashMap<>();
    }

    public String getId() { return id.get(); }
    public StringProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getCourse() { return course.get(); }
    public StringProperty courseProperty() { return course; }

    public double getOverallRate() { return overallRate.get(); }
    public DoubleProperty overallRateProperty() { return overallRate; }

    public void addStatusForDate(String date, String status, String reason) {
        if (status == null || status.trim().isEmpty()) {
            this.dailyMap.remove(date);
            this.dailyReasonMap.remove(date);
        } else {
            this.dailyMap.put(date, status);
            if (reason != null && !reason.trim().isEmpty()) {
                this.dailyReasonMap.put(date, reason);
            } else {
                this.dailyReasonMap.remove(date);
            }
        }
        recalculateOverallRate();
    }

    public String getStatusForDate(String date) {
        return this.dailyMap.getOrDefault(date, "");
    }

    public String getReasonForDate(String date) {
        return this.dailyReasonMap.getOrDefault(date, "");
    }

    public Map<String, String> getDailyMap() {
        return dailyMap;
    }

    /**
     * Recalculates overall attendance rate dynamically.
     * Formula: ((Present + Late) / Total Logged Classes) * 100
     */
    public void recalculateOverallRate() {
        if (dailyMap.isEmpty()) {
            overallRate.set(100.0);
            return;
        }

        long presentOrLateCount = 0;
        long totalLoggedCount = 0;

        for (String status : dailyMap.values()) {
            if (status != null && !status.trim().isEmpty()) {
                totalLoggedCount++;
                if ("Present".equalsIgnoreCase(status) || "Late".equalsIgnoreCase(status)) {
                    presentOrLateCount++;
                }
            }
        }

        if (totalLoggedCount == 0) {
            overallRate.set(100.0);
        } else {
            double rate = ((double) presentOrLateCount / totalLoggedCount) * 100.0;
            overallRate.set(rate);
        }
    }
}