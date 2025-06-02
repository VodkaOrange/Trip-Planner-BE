package com.tripplanner.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ItineraryRequest {

    @NotBlank(message = "Destination cannot be blank")
    private String destination;

    @NotNull(message = "Number of days cannot be null")
    @Min(value = 1, message = "Number of days must be at least 1")
    private Integer numberOfDays;

    private String budgetRange;

    @AssertTrue(message = "Terms and conditions must be accepted to create an itinerary")
    private boolean termsAccepted;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(Integer numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public String getBudgetRange() {
        return budgetRange;
    }

    public void setBudgetRange(String budgetRange) {
        this.budgetRange = budgetRange;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }
}
