package com.tripplanner.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class AddActivityRequest {

    @NotBlank(message = "Activity name cannot be blank")
    private String name;

    private String city;
    private String description;

    @NotNull(message = "Expected duration cannot be null")
    @Min(value = 0, message = "Expected duration must be positive or zero")
    private Double expectedDurationHours;

    @NotNull(message = "Estimated cost cannot be null")
    @PositiveOrZero(message = "Estimated cost must be positive or zero")
    private Double estimatedCostEUR;

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

    public Double getExpectedDurationHours() {
        return expectedDurationHours;
    }

    public void setExpectedDurationHours(Double expectedDurationHours) {
        this.expectedDurationHours = expectedDurationHours;
    }

    public Double getEstimatedCostEUR() {
        return estimatedCostEUR;
    }

    public void setEstimatedCostEUR(Double estimatedCostEUR) {
        this.estimatedCostEUR = estimatedCostEUR;
    }
}
