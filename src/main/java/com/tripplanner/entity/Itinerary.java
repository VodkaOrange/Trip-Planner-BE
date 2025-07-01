package com.tripplanner.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "itineraries")
public class Itinerary {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  private String destination;

  @Column(unique = true)
  private String shareableLink;

  private LocalDateTime createdAt;

  @Column(name = "is_finalized", nullable = false)
  private boolean finalized = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id") // Nullable so that non-logged-in users can create (but not save)
  private User user;

  @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("dayNumber ASC")
  private List<DayPlan> dayPlans = new ArrayList<>();

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(name = "itinerary_interests",
      joinColumns = @JoinColumn(name = "itinerary_id"),
      inverseJoinColumns = @JoinColumn(name = "interest_id"))
  private Set<Interest> interests = new HashSet<>();

  @Column
  private String departureCity;

  @Column
  private Integer numberOfChildren;

  @Column
  private Integer numberOfAdults;

  @Column
  private LocalDate fromDate;

  @Column
  private LocalDate toDate;

  @PrePersist
  protected void onCreate() {

    createdAt = LocalDateTime.now();
  }

  public Itinerary() {

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

  public boolean isFinalized() {

    return finalized;
  }

  public void setFinalized(boolean finalized) {

    this.finalized = finalized;
  }

  public User getUser() {

    return user;
  }

  public void setUser(User user) {

    this.user = user;
  }

  public List<DayPlan> getDayPlans() {

    return dayPlans;
  }

  public void addDayPlan(DayPlan dayPlan) {

    dayPlans.add(dayPlan);
    dayPlan.setItinerary(this);
  }

  public Set<Interest> getInterests() {

    return interests;
  }

  public void setDayPlans(List<DayPlan> dayPlans) {

    this.dayPlans = dayPlans;
  }

  public void setInterests(Set<Interest> interests) {

    this.interests = interests;
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
}
