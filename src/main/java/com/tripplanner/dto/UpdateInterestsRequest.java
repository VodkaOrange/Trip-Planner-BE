package com.tripplanner.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public class UpdateInterestsRequest {

    @NotEmpty(message = "Interest names cannot be empty")
    private Set<String> interestNames;

    public Set<String> getInterestNames() {
        return interestNames;
    }

    public void setInterestNames(Set<String> interestNames) {
        this.interestNames = interestNames;
    }
}
