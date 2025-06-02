package com.tripplanner.dto;

import com.tripplanner.entity.Interest;

public class InterestDto {
    private Long id;
    private String name;

    public InterestDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static InterestDto fromEntity(Interest interest) {
        return new InterestDto(interest.getId(), interest.getName());
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
}
