package com.tripplanner.controller;

import com.tripplanner.dto.AddActivityRequest;
import com.tripplanner.dto.DestinationPreferencesRequest;
import com.tripplanner.dto.InterestDto;
import com.tripplanner.dto.ItineraryRequest;
import com.tripplanner.dto.ItineraryResponse;
import com.tripplanner.dto.UpdateInterestsRequest;
import com.tripplanner.dto.ai.SuggestedActivityDto;
import com.tripplanner.dto.ai.SuggestedCountryDto;
import com.tripplanner.entity.Itinerary;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.service.ItineraryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ItineraryController {

  private final ItineraryService itineraryService;

  public ItineraryController(ItineraryService itineraryService) {

    this.itineraryService = itineraryService;
  }

  @PostMapping("/trip/start")
  public ResponseEntity<ItineraryResponse> createNewItinerary(@Valid @RequestBody ItineraryRequest itineraryRequest) {

    Itinerary itinerary = itineraryService.createItinerary(itineraryRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(ItineraryResponse.fromEntity(itinerary));
  }

  @PostMapping("/trip/suggestions/countries")
  public ResponseEntity<List<SuggestedCountryDto>> suggestCountriesByPreferences(
      @Valid @RequestBody DestinationPreferencesRequest preferencesRequest) {

    List<SuggestedCountryDto> suggestions = itineraryService.suggestCountries(preferencesRequest.getPreferences());
    return ResponseEntity.ok(suggestions);
  }

  @GetMapping("/trip/{id}")
  public ResponseEntity<ItineraryResponse> getItinerary(@PathVariable Long id) {

    Itinerary itinerary = itineraryService.getItineraryById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));
    return ResponseEntity.ok(ItineraryResponse.fromEntity(itinerary));
  }

  @PostMapping("/trip/{itineraryId}/interests")
  public ResponseEntity<ItineraryResponse> updateItineraryInterests(@PathVariable Long itineraryId,
      @Valid @RequestBody UpdateInterestsRequest interestsRequest) {

    Itinerary updatedItinerary = itineraryService.updateInterests(itineraryId, interestsRequest);
    return ResponseEntity.ok(ItineraryResponse.fromEntity(updatedItinerary));
  }

  @GetMapping("/trip/interests")
  public ResponseEntity<List<InterestDto>> listAllInterests() {

    List<InterestDto> interestDtos = itineraryService.getAllInterestsAsDto();
    return ResponseEntity.ok(interestDtos);
  }

  @PostMapping("/trip/{itineraryId}/days/{dayNumber}/activities")
  public ResponseEntity<ItineraryResponse> addActivityToDayPlan(@PathVariable Long itineraryId,
      @PathVariable int dayNumber,
      @Valid @RequestBody AddActivityRequest activityRequest) {

    Itinerary updatedItinerary = itineraryService.addActivityToDay(itineraryId, dayNumber, activityRequest);
    return ResponseEntity.ok(ItineraryResponse.fromEntity(updatedItinerary));
  }

  @GetMapping("/trip/{itineraryId}/days/{dayNumber}/suggestions/activities")
  public ResponseEntity<List<SuggestedActivityDto>> suggestActivitiesForDay(
      @PathVariable Long itineraryId,
      @PathVariable int dayNumber) {

    List<SuggestedActivityDto> suggestions = itineraryService.suggestActivitiesForDay(itineraryId, dayNumber);
    return ResponseEntity.ok(suggestions);
  }

  @PostMapping("/trip/{itineraryId}/finalize")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ItineraryResponse> finalizeItinerary(@PathVariable Long itineraryId) {

    Itinerary finalizedItinerary = itineraryService.finalizeItinerary(itineraryId);
    return ResponseEntity.ok(ItineraryResponse.fromEntity(finalizedItinerary));
  }

  @GetMapping("/shared/{shareableLink}")
  public ResponseEntity<ItineraryResponse> getSharedItinerary(@PathVariable String shareableLink) {

    Itinerary itinerary = itineraryService.getSharedItinerary(shareableLink);
    return ResponseEntity.ok(ItineraryResponse.fromEntity(itinerary));
  }
}