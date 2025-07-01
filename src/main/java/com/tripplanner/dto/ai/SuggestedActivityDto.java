package com.tripplanner.dto.ai;

// DTO for an activity suggestion from the AI
public class SuggestedActivityDto {
    private String name;
    private String city;
    private String description;
    private double expectedDurationHours;
    private double estimatedCostEUR;
    private String image;
    private String address;

    public SuggestedActivityDto() {}

    public SuggestedActivityDto(String name, String city, String description, double expectedDurationHours, double estimatedCostEUR,
        String image, String address) {
        this.name = name;
        this.city = city;
        this.description = description;
        this.expectedDurationHours = expectedDurationHours;
        this.estimatedCostEUR = estimatedCostEUR;
        this.image = image;
        this.address = address;
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

    public String getImage() {

        return image;
    }

    public void setImage(String image) {

        this.image = image;
    }

    public String getAddress() {

        return address;
    }

    public void setAddress(String address) {

        this.address = address;
    }
}
