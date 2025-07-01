package com.tripplanner.dto;

import com.tripplanner.entity.Itinerary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ItineraryResponse {

  private Long id;
  private String destination;
  private String shareableLink;
  private LocalDateTime createdAt;
  private Long userId; // Only include if user is present
  private boolean finalized; // New field
  private List<DayPlanDto> dayPlans;
  private Set<InterestDto> interests;

  public ItineraryResponse(Long id, String destination,
      String shareableLink, LocalDateTime createdAt, Long userId,
      boolean finalized, List<DayPlanDto> dayPlans, Set<InterestDto> interests) {

    this.id = id;
    this.destination = destination;
    this.shareableLink = shareableLink;
    this.createdAt = createdAt;
    this.userId = userId;
    this.finalized = finalized;
    this.dayPlans = dayPlans;
    this.interests = interests;
  }

  public static ItineraryResponse fromEntity(Itinerary itinerary) {

    return new ItineraryResponse(
        itinerary.getId(),
        itinerary.getDestination(),
        itinerary.getShareableLink(),
        itinerary.getCreatedAt(),
        itinerary.getUser() != null ? itinerary.getUser().getId() : null,
        itinerary.isFinalized(),
        itinerary.getDayPlans().stream().map(DayPlanDto::fromEntity).collect(Collectors.toList()),
        itinerary.getInterests().stream().map(InterestDto::fromEntity).collect(Collectors.toSet())
    );
  }

  public Long getId() {

    return id;
  }

  public void setId(Long id) {

    this.id = id;
  }

  public String getDestination() {

    return destination;
  }

  public void setDestination(String destination) {

    this.destination = destination;
  }

  public String getShareableLink() {

    return shareableLink;
  }

  public void setShareableLink(String shareableLink) {

    this.shareableLink = shareableLink;
  }

  public LocalDateTime getCreatedAt() {

    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {

    this.createdAt = createdAt;
  }

  public Long getUserId() {

    return userId;
  }

  public void setUserId(Long userId) {

    this.userId = userId;
  }

  public boolean isFinalized() {

    return finalized;
  }

  public void setFinalized(boolean finalized) {

    this.finalized = finalized;
  }

  public List<DayPlanDto> getDayPlans() {

    return dayPlans;
  }

  public void setDayPlans(List<DayPlanDto> dayPlans) {

    this.dayPlans = dayPlans;
  }

  public Set<InterestDto> getInterests() {

    return interests;
  }

  public void setInterests(Set<InterestDto> interests) {

    this.interests = interests;
  }
}
