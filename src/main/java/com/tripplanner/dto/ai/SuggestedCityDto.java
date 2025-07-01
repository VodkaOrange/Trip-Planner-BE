package com.tripplanner.dto.ai;

// DTO for a country suggestion from the AI
public class SuggestedCityDto {
    private String country;

    private String city;
    private String overview;
    private String imageUrl;

    public SuggestedCityDto() {}

    public SuggestedCityDto(String city, String country, String overview, String imageUrl) {
        this.country = country;
        this.overview = overview;
        this.imageUrl = imageUrl;
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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
