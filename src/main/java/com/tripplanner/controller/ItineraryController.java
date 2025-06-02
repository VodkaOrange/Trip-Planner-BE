package com.tripplanner.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.*;
import com.tripplanner.dto.ai.AiErrorDto;
import com.tripplanner.dto.ai.SuggestedActivityDto;
import com.tripplanner.dto.ai.SuggestedCountryDto;
import com.tripplanner.entity.Activity;
import com.tripplanner.entity.DayPlan;
import com.tripplanner.entity.Interest;
import com.tripplanner.entity.Itinerary;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.service.GoogleAiService;
import com.tripplanner.service.ItineraryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ItineraryController {

    private static final Logger logger = LoggerFactory.getLogger(ItineraryController.class);

    @Value("${app.planning.max-schedulable-hours-per-day:10.0}") // Default to 10 hours
    private double maxSchedulableHoursPerDay;

    @Autowired
    private ItineraryService itineraryService;

    @Autowired
    private GoogleAiService googleAiService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/trip/start")
    public ResponseEntity<?> createNewItinerary(@Valid @RequestBody ItineraryRequest itineraryRequest) {
        Itinerary itinerary = itineraryService.createItinerary(itineraryRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItineraryResponse.fromEntity(itinerary));
    }

    @PostMapping("/trip/suggestions/countries")
    public ResponseEntity<?> suggestCountriesByPreferences(@Valid @RequestBody DestinationPreferencesRequest preferencesRequest) {
        String aiResponse = "";
        try {
            aiResponse = googleAiService.suggestCountries(preferencesRequest.getPreferences());
            logger.debug("AI Response for country suggestions: {}", aiResponse);
            List<SuggestedCountryDto> suggestions = objectMapper.readValue(aiResponse, new TypeReference<List<SuggestedCountryDto>>() {});
            return ResponseEntity.ok(suggestions);
        } catch (JsonProcessingException jpe) {
            logger.error("JSON Parsing Error for country suggestions AI Response: '{}', Exception: {}", aiResponse, jpe.getMessage());
            try {
                AiErrorDto errorDto = objectMapper.readValue(aiResponse, AiErrorDto.class);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
            } catch (JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AiErrorDto("Invalid AI response format for country suggestions."));
            }
        } 
    }

    @GetMapping("/trip/{id}")
    public ResponseEntity<?> getItinerary(@PathVariable Long id) {
        Itinerary itinerary = itineraryService.getItineraryById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", id));
        return ResponseEntity.ok(ItineraryResponse.fromEntity(itinerary));
    }

    @PostMapping("/trip/{itineraryId}/interests")
    public ResponseEntity<?> updateItineraryInterests(@PathVariable Long itineraryId, @Valid @RequestBody UpdateInterestsRequest interestsRequest) {
        Itinerary updatedItinerary = itineraryService.updateInterests(itineraryId, interestsRequest);
        return ResponseEntity.ok(ItineraryResponse.fromEntity(updatedItinerary));
    }

    @GetMapping("/trip/interests")
    public ResponseEntity<List<InterestDto>> listAllInterests() {
        List<Interest> interests = itineraryService.getAllInterests();
        List<InterestDto> interestDtos = interests.stream()
                                                .map(InterestDto::fromEntity)
                                                .collect(Collectors.toList());
        return ResponseEntity.ok(interestDtos);
    }

    @PostMapping("/trip/{itineraryId}/days/{dayNumber}/activities")
    public ResponseEntity<?> addActivityToDayPlan(@PathVariable Long itineraryId, 
                                                @PathVariable int dayNumber, 
                                                @Valid @RequestBody AddActivityRequest activityRequest) {
        Itinerary updatedItinerary = itineraryService.addActivityToDay(itineraryId, dayNumber, activityRequest);
        return ResponseEntity.ok(ItineraryResponse.fromEntity(updatedItinerary)); 
    }

    @GetMapping("/trip/{itineraryId}/days/{dayNumber}/suggestions/activities")
    public ResponseEntity<?> suggestActivitiesForDay(
            @PathVariable Long itineraryId,
            @PathVariable int dayNumber) {
        String aiResponse = "";
        try {
            Itinerary itinerary = itineraryService.getItineraryById(itineraryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

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
                lastActivity = currentDayPlan.getActivities().get(currentDayPlan.getActivities().size() - 1);
            }

            List<String> interestNames = itinerary.getInterests().stream()
                                                .map(Interest::getName)
                                                .collect(Collectors.toList());
            
            List<String> previousActivitiesForDayNames = currentDayPlan.getActivities().stream()
                .map(Activity::getName)
                .collect(Collectors.toList());

            aiResponse = googleAiService.suggestActivities(
                    itinerary.getDestination(),
                    interestNames,
                    itinerary.getBudgetRange(),
                    dayNumber,
                    itinerary.getNumberOfDays(),
                    previousActivitiesForDayNames,
                    availableHoursToday,
                    lastActivity != null ? lastActivity.getCity() : null,
                    lastActivity != null ? lastActivity.getName() : null
            );
            logger.debug("AI Response for activity suggestions: {}", aiResponse);

            List<SuggestedActivityDto> suggestions = objectMapper.readValue(aiResponse, new TypeReference<List<SuggestedActivityDto>>() {});
            return ResponseEntity.ok(suggestions);

        } catch (JsonProcessingException jpe) {
            logger.error("JSON Parsing Error for activity suggestions AI Response: '{}', Exception: {}", aiResponse, jpe.getMessage());
             try {
                AiErrorDto errorDto = objectMapper.readValue(aiResponse, AiErrorDto.class);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
            } catch (JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AiErrorDto("Invalid AI response format for activity suggestions."));
            }
        } 
    }
    
    @PostMapping("/trip/{itineraryId}/finalize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> finalizeItinerary(@PathVariable Long itineraryId) {
        Itinerary finalizedItinerary = itineraryService.finalizeItinerary(itineraryId);
        return ResponseEntity.ok(ItineraryResponse.fromEntity(finalizedItinerary));
    }

    @GetMapping("/shared/{shareableLink}")
    public ResponseEntity<?> getSharedItinerary(@PathVariable String shareableLink) {
        Optional<Itinerary> itineraryOptional = itineraryService.getItineraryByShareableLink(shareableLink);
        Itinerary itinerary = itineraryOptional.orElseThrow(() -> new ResourceNotFoundException("Shared itinerary not found with link: " + shareableLink));
        
        if (itinerary.isFinalized()) { 
            return ResponseEntity.ok(ItineraryResponse.fromEntity(itinerary));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AiErrorDto("This itinerary is not finalized or not available for sharing."));
    }
}
