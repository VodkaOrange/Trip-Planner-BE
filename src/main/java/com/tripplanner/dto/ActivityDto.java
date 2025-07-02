package com.tripplanner.dto;

import com.tripplanner.entity.Activity;

public class ActivityDto {
    private Long id;
    private String name;
    private String city;
    private String description;
    private String address;
    private double expectedDurationHours;
    private double estimatedCostEUR;

    public ActivityDto(Long id, String name, String city, String description, String address, double expectedDurationHours,
        double estimatedCostEUR) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.description = description;
        this.address = address;
        this.expectedDurationHours = expectedDurationHours;
        this.estimatedCostEUR = estimatedCostEUR;
    }

    public static ActivityDto fromEntity(Activity activity) {
        return new ActivityDto(
                activity.getId(),
                activity.getName(),
                activity.getCity(),
                activity.getDescription(),
                activity.getAddress(),
                activity.getExpectedDurationHours(),
                activity.getEstimatedCostEUR()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {return address;}

    public void setAddress(String address) {this.address = address; }

    public double getExpectedDurationHours() {
        return expectedDurationHours;
    }

    public void setExpectedDurationHours(double expectedDurationHours) {
        this.expectedDurationHours = expectedDurationHours;
    }

    public double getEstimatedCostEUR() {
        return estimatedCostEUR;
    }

    public void setEstimatedCostEUR(double estimatedCostEUR) {
        this.estimatedCostEUR = estimatedCostEUR;
    }
}
