package com.tripplanner.dto.ai;

// DTO for a country suggestion from the AI
public class SuggestedCountryDto {
    private String country;
    private String overview;
    private String imageUrl;

    public SuggestedCountryDto() {}

    public SuggestedCountryDto(String country, String overview, String imageUrl) {
        this.country = country;
        this.overview = overview;
        this.imageUrl = imageUrl;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
