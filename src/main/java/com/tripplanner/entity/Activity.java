package com.tripplanner.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String city;
    private String description;
    private String address;
    private double expectedDurationHours;

    @PositiveOrZero
    private double estimatedCostEUR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_plan_id")
    private DayPlan dayPlan;

    public Activity() {
    }

    public Activity(String name, String city, String description, String address, double expectedDurationHours,
        double estimatedCostEUR) {
        this.name = name;
        this.city = city;
        this.description = description;
        this.address = address;
        this.expectedDurationHours = expectedDurationHours;
        this.estimatedCostEUR = estimatedCostEUR;
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

    public DayPlan getDayPlan() {
        return dayPlan;
    }

    public void setDayPlan(DayPlan dayPlan) {
        this.dayPlan = dayPlan;
    }
}
