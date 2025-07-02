package com.tripplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.AddActivityRequest;
import com.tripplanner.dto.InterestDto;
import com.tripplanner.dto.ItineraryRequest;
import com.tripplanner.dto.UpdateInterestsRequest;
import com.tripplanner.dto.ai.AiErrorDto;
import com.tripplanner.dto.ai.SuggestedActivityDto;
import com.tripplanner.dto.ai.SuggestedCityDto;
import com.tripplanner.entity.Activity;
import com.tripplanner.entity.DayPlan;
import com.tripplanner.entity.Interest;
import com.tripplanner.entity.Itinerary;
import com.tripplanner.entity.User;
import com.tripplanner.exception.AiServiceException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.DayPlanRepository;
import com.tripplanner.repository.InterestRepository;
import com.tripplanner.repository.ItineraryRepository;
import com.tripplanner.repository.UserRepository;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryService {

  private static final String EXCEPTION_MESSAGE_ITINERARY_NOT_FOUND = "Itinerary with id %s not found";
  private static final Logger logger = LoggerFactory.getLogger(ItineraryService.class);

  @Value("${app.planning.max-schedulable-hours-per-day:10.0}")
  private double maxSchedulableHoursPerDay;

  private final ItineraryRepository itineraryRepository;

  private final UserRepository userRepository;

  private final InterestRepository interestRepository;

  private final DayPlanRepository dayPlanRepository;

  private final GoogleAiService googleAiService;

  private final ObjectMapper objectMapper;

  private final GoogleCseService googleCseService;

  public ItineraryService(ItineraryRepository itineraryRepository, UserRepository userRepository,
      InterestRepository interestRepository, DayPlanRepository dayPlanRepository,
      GoogleAiService googleAiService, ObjectMapper objectMapper, GoogleCseService googleCseService) {

    this.itineraryRepository = itineraryRepository;
    this.userRepository = userRepository;
    this.interestRepository = interestRepository;
    this.dayPlanRepository = dayPlanRepository;
    this.googleAiService = googleAiService;
    this.objectMapper = objectMapper;
    this.googleCseService = googleCseService;
  }

  @Transactional
  public Itinerary createItinerary(ItineraryRequest itineraryRequest) {

    Itinerary itinerary = new Itinerary();
    itinerary.setDestination(itineraryRequest.getDestination());
    itinerary.setFinalized(false);
    itinerary.setDepartureCity(itineraryRequest.getDepartureCity());
    itinerary.setNumberOfAdults(itineraryRequest.getNumberOfAdults());
    itinerary.setNumberOfChildren(itineraryRequest.getNumberOfChildren());
    itinerary.setFromDate(itineraryRequest.getFromDate());
    itinerary.setToDate(itineraryRequest.getToDate());

    Set<Interest> interestsToSave = new HashSet<>();
    if (itineraryRequest.getInterests() != null) {
      for (Interest interest : itineraryRequest.getInterests()) {
        if (interest.getName() != null) {
          Optional<Interest> existingInterest = interestRepository.findByName(interest.getName());
          existingInterest.ifPresent(interestsToSave::add);
        }
      }
    }

    itinerary.setInterests(interestsToSave);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String
        && authentication.getPrincipal().equals("anonymousUser"))) {
      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
      Optional<User> userOptional = userRepository.findById(userDetails.getId());
      userOptional.ifPresent(itinerary::setUser);
    }

    int numberOfDays = (int) ChronoUnit.DAYS.between(itineraryRequest.getFromDate(), itineraryRequest.getToDate()) + 1;

    for (int i = 1; i <= numberOfDays; i++) {
      DayPlan dayPlan = new DayPlan(i);
      itinerary.addDayPlan(dayPlan);
    }

    return itineraryRepository.save(itinerary);
  }

  public Optional<Itinerary> getItineraryById(Long id) {

    return itineraryRepository.findById(id);
  }

  public Optional<Itinerary> getItineraryByShareableLink(String shareableLink) {

    return itineraryRepository.findByShareableLink(shareableLink);
  }

  @Transactional
  public Itinerary updateInterests(Long itineraryId, UpdateInterestsRequest interestsRequest) {

    Itinerary itinerary = itineraryRepository.findById(itineraryId)
        .orElseThrow(() -> new ResourceNotFoundException(String.format(EXCEPTION_MESSAGE_ITINERARY_NOT_FOUND, itineraryId)));

    if (itinerary.isFinalized()) {
      throw new IllegalStateException("Cannot update interests for a finalized itinerary.");
    }
    Set<Interest> newInterests = new HashSet<>();
    for (String interestName : interestsRequest.getInterestNames()) {
      Interest interest = interestRepository.findByName(interestName)
          .orElseGet(() -> interestRepository.save(new Interest(interestName)));
      newInterests.add(interest);
    }

    itinerary.getInterests().clear();
    itinerary.getInterests().addAll(newInterests);
    for (Interest interest : newInterests) {
      interest.getItineraries().add(itinerary);
    }

    return itineraryRepository.save(itinerary);
  }

  public List<InterestDto> getAllInterestsAsDto() {

    List<Interest> interests = interestRepository.findAll();
    return interests.stream()
        .map(InterestDto::fromEntity)
        .toList();
  }

  @Transactional
  public Itinerary addActivityToDay(Long itineraryId, int dayNumber, AddActivityRequest activityRequest) {

    Itinerary itinerary = itineraryRepository.findById(itineraryId)
        .orElseThrow(() -> new ResourceNotFoundException(String.format(EXCEPTION_MESSAGE_ITINERARY_NOT_FOUND, itineraryId)));

    if (itinerary.isFinalized()) {
      throw new IllegalStateException("Cannot add activities to a finalized itinerary.");
    }

    int numberOfDays = (int) ChronoUnit.DAYS.between(itinerary.getFromDate(), itinerary.getToDate()) + 1;

    if (dayNumber <= 0 || dayNumber > numberOfDays) {
      throw new IllegalArgumentException("Invalid day number: " + dayNumber +
          " for an itinerary with " + numberOfDays + " days.");
    }

    DayPlan dayPlan = itinerary.getDayPlans().stream()
        .filter(dp -> dp.getDayNumber() == dayNumber)
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(
            "DayPlan not found for day number: " + dayNumber + " in itinerary id: " + itineraryId));

    Activity activity = new Activity(
        activityRequest.getName(),
        activityRequest.getCity(),
        activityRequest.getDescription(),
        activityRequest.getAddress(),
        activityRequest.getExpectedDurationHours(),
        activityRequest.getEstimatedCostEUR()
    );

    dayPlan.addActivity(activity);
    dayPlanRepository.save(dayPlan);

    return itineraryRepository.findById(itineraryId)
        .orElseThrow(() -> new ResourceNotFoundException(String.format(EXCEPTION_MESSAGE_ITINERARY_NOT_FOUND, itineraryId)));
  }

  @Transactional
  public Itinerary finalizeItinerary(Long itineraryId) {

    Itinerary itinerary = itineraryRepository.findById(itineraryId)
        .orElseThrow(() -> new ResourceNotFoundException(String.format(EXCEPTION_MESSAGE_ITINERARY_NOT_FOUND, itineraryId)));

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || (authentication.getPrincipal() instanceof String
        && authentication.getPrincipal().equals("anonymousUser"))) {
      throw new AccessDeniedException("User must be logged in to finalize an itinerary.");
    }

    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    Long currentUserId = userDetails.getId();

    if (itinerary.getUser() == null) {
      User currentUser = userRepository.findById(currentUserId)
          .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
      itinerary.setUser(currentUser);
    }
    else if (!itinerary.getUser().getId().equals(currentUserId)) {
      throw new AccessDeniedException("User not authorized to finalize this itinerary. It is owned by another user.");
    }

    if (itinerary.isFinalized()) {
      return itinerary;
    }

    itinerary.setFinalized(true);
    if (itinerary.getShareableLink() == null || itinerary.getShareableLink().isEmpty()) {
      itinerary.setShareableLink(UUID.randomUUID().toString());
    }

    return itineraryRepository.save(itinerary);
  }

  public List<SuggestedCityDto> suggestCities(List<String> preferences) throws AiServiceException {

    String aiResponse = googleAiService.suggestCities(preferences);
    logger.debug("AI Response for cities suggestions: {}", aiResponse);
    try {
      List<SuggestedCityDto> cities = objectMapper.readValue(aiResponse, new TypeReference<>() {
      });
      cities.forEach(
          city ->
              city.setImageUrl(googleCseService.searchImages(city.getCity() + ", " + city.getCountry()).getFirst()));
      return cities;
    }
    catch (JsonProcessingException jpe) {
      logger.error("JSON Parsing Error for country suggestions AI Response: '{}', Exception: {}", aiResponse, jpe.getMessage());
      try {
        AiErrorDto errorDto = objectMapper.readValue(aiResponse, AiErrorDto.class);
        throw new AiServiceException(errorDto.getError());
      }
      catch (JsonProcessingException e) {
        throw new AiServiceException("Invalid AI response format for country suggestions.");
      }
    }
  }

  public List<SuggestedActivityDto> suggestActivitiesForDay(Long itineraryId, int dayNumber) throws AiServiceException {

    Itinerary itinerary = getItineraryById(itineraryId)
        .orElseThrow(() -> new ResourceNotFoundException(String.format(EXCEPTION_MESSAGE_ITINERARY_NOT_FOUND, itineraryId)));

    if (itinerary.isFinalized()) {
      throw new IllegalStateException("Cannot get activity suggestions for a finalized itinerary.");
    }

    DayPlan currentDayPlan = itinerary.getDayPlans().stream()
        .filter(dp -> dp.getDayNumber() == dayNumber)
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("DayPlan", "dayNumber", dayNumber));

    double hoursTakenToday = currentDayPlan.getActivities().stream()
        .mapToDouble(Activity::getExpectedDurationHours)
        .sum();
    double availableHoursToday = Math.max(0, maxSchedulableHoursPerDay - hoursTakenToday);

    Activity lastActivity = null;
    if (!currentDayPlan.getActivities().isEmpty()) {
      lastActivity = currentDayPlan.getActivities().getLast();
    }

    List<String> interestNames = itinerary.getInterests().stream()
        .map(Interest::getName)
        .toList();

    List<String> previousActivitiesForDayNames = currentDayPlan.getActivities().stream()
        .map(Activity::getName)
        .toList();

    int numberOfDays = (int) ChronoUnit.DAYS.between(itinerary.getFromDate(), itinerary.getToDate()) + 1;


    String aiResponse = googleAiService.suggestActivities(
        itinerary.getDestination(),
        interestNames,
        itinerary.getNumberOfAdults(),
        itinerary.getNumberOfChildren(),
        itinerary.getFromDate(),
        itinerary.getToDate(),
        currentDayPlan.getDayNumber(), // dayNumber
        numberOfDays,
        previousActivitiesForDayNames,
        availableHoursToday,
        lastActivity != null ? lastActivity.getCity() : null,
        lastActivity != null ? lastActivity.getName() : null,
        itinerary.getDepartureCity() // Optional, may be null
    );
    logger.debug("AI Response for activity suggestions: {}", aiResponse);

    try {
      List<SuggestedActivityDto> suggestedActivities = objectMapper.readValue(aiResponse, new TypeReference<>() {
      });
      suggestedActivities.forEach(activity ->
          activity.setImage(
              this.googleCseService.searchImages(activity.getName() + " " + activity.getDescription()).getFirst()));
      return suggestedActivities;
    }
    catch (JsonProcessingException jpe) {
      logger.error("JSON Parsing Error for activity suggestions AI Response: '{}', Exception: {}", aiResponse,
          jpe.getMessage());
      try {
        AiErrorDto errorDto = objectMapper.readValue(aiResponse, AiErrorDto.class);
        throw new AiServiceException(errorDto.getError());
      }
      catch (JsonProcessingException e) {
        throw new AiServiceException("Invalid AI response format for activity suggestions.");
      }
    }
  }

  public Itinerary getSharedItinerary(String shareableLink) {

    Itinerary itinerary = getItineraryByShareableLink(shareableLink)
        .orElseThrow(() -> new ResourceNotFoundException("Shared itinerary not found with link: " + shareableLink));
    if (!itinerary.isFinalized()) {
      throw new AccessDeniedException("This itinerary is not finalized or not available for sharing.");
    }
    return itinerary;
  }
}