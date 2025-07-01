package com.tripplanner.dto;

import com.tripplanner.entity.Interest;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class ItineraryRequest {

  @NotBlank(message = "Destination cannot be blank")
  private String destination;

  private String departureCity;

  private Integer numberOfChildren;

  private Integer numberOfAdults;

  private LocalDate fromDate;

  private LocalDate toDate;

  private Set<Interest> interests = new HashSet<>();

  public String getDestination() {

    return destination;
  }

  public void setDestination(String destination) {

    this.destination = destination;
  }

  public String getDepartureCity() {

    return departureCity;
  }

  public void setDepartureCity(String departureCity) {

    this.departureCity = departureCity;
  }

  public Integer getNumberOfChildren() {

    return numberOfChildren;
  }

  public void setNumberOfChildren(Integer numberOfChildren) {

    this.numberOfChildren = numberOfChildren;
  }

  public Integer getNumberOfAdults() {

    return numberOfAdults;
  }

  public void setNumberOfAdults(Integer numberOfAdults) {

    this.numberOfAdults = numberOfAdults;
  }

  public LocalDate getFromDate() {

    return fromDate;
  }

  public void setFromDate(LocalDate fromDate) {

    this.fromDate = fromDate;
  }

  public LocalDate getToDate() {

    return toDate;
  }

  public void setToDate(LocalDate toDate) {

    this.toDate = toDate;
  }

  public Set<Interest> getInterests() {

    return interests;
  }

  public void setInterests(Set<Interest> interests) {

    this.interests = interests;
  }
}
