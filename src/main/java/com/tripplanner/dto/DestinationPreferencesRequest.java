package com.tripplanner.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class DestinationPreferencesRequest {

    @NotEmpty(message = "Preferences cannot be empty")
    private List<String> preferences; // e.g., ["nature", "architecture", "ancient civilizations", "weather:sunny"]

    public List<String> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
    }
}
