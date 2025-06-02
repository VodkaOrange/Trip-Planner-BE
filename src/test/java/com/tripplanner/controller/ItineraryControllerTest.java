package com.tripplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.DestinationPreferencesRequest;
import com.tripplanner.dto.ItineraryRequest;
import com.tripplanner.dto.ai.SuggestedCountryDto;
import com.tripplanner.entity.Itinerary;
import com.tripplanner.service.GoogleAiService;
import com.tripplanner.service.ItineraryService;
import com.tripplanner.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItineraryController.class)
@Import(GlobalExceptionHandler.class) 
public class ItineraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; 

    @MockBean
    private ItineraryService itineraryService;

    @MockBean
    private GoogleAiService googleAiService; 

    @Test
    public void whenCreateNewItinerary_withValidRequest_thenReturnCreatedItinerary() throws Exception {
        ItineraryRequest request = new ItineraryRequest();
        request.setDestination("Paris");
        request.setNumberOfDays(7);
        request.setBudgetRange("EUR 1000-2000");
        request.setTermsAccepted(true);

        Itinerary createdItinerary = new Itinerary();
        createdItinerary.setId(1L);
        createdItinerary.setDestination(request.getDestination());
        createdItinerary.setNumberOfDays(request.getNumberOfDays());
        createdItinerary.setBudgetRange(request.getBudgetRange());
        createdItinerary.setTermsAccepted(true);
        createdItinerary.setCreatedAt(LocalDateTime.now());
        createdItinerary.setFinalized(false);

        when(itineraryService.createItinerary(any(ItineraryRequest.class))).thenReturn(createdItinerary);

        mockMvc.perform(post("/api/trip/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.destination").value("Paris"))
                .andExpect(jsonPath("$.numberOfDays").value(7))
                .andExpect(jsonPath("$.budgetRange").value("EUR 1000-2000"))
                .andExpect(jsonPath("$.termsAccepted").value(true))
                .andExpect(jsonPath("$.finalized").value(false));
    }

    @Test
    public void whenCreateNewItinerary_withMissingDestination_thenReturnBadRequest() throws Exception {
        ItineraryRequest request = new ItineraryRequest();
        request.setNumberOfDays(5);
        request.setTermsAccepted(true);

        mockMvc.perform(post("/api/trip/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Destination cannot be blank")));
    }

    @Test
    public void whenCreateNewItinerary_withTermsNotAccepted_thenReturnBadRequest() throws Exception {
        ItineraryRequest request = new ItineraryRequest();
        request.setDestination("Rome");
        request.setNumberOfDays(3);
        request.setTermsAccepted(false); 

        mockMvc.perform(post("/api/trip/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Terms and conditions must be accepted")));
    }
    
    @Test
    public void whenCreateNewItinerary_withInvalidNumberOfDays_thenReturnBadRequest() throws Exception {
        ItineraryRequest request = new ItineraryRequest();
        request.setDestination("Berlin");
        request.setNumberOfDays(0); 
        request.setTermsAccepted(true);

        mockMvc.perform(post("/api/trip/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Number of days must be at least 1")));
    }

    // Tests for suggestCountriesByPreferences endpoint
    @Test
    public void whenSuggestCountries_withValidPreferences_thenReturnSuggestions() throws Exception {
        DestinationPreferencesRequest preferencesRequest = new DestinationPreferencesRequest();
        preferencesRequest.setPreferences(List.of("nature", "history"));

        List<SuggestedCountryDto> mockSuggestions = List.of(
            new SuggestedCountryDto("CountryA", "Overview A", "placeholder_image_for_CountryA"),
            new SuggestedCountryDto("CountryB", "Overview B", "placeholder_image_for_CountryB")
        );
        String mockJsonResponse = objectMapper.writeValueAsString(mockSuggestions);

        when(googleAiService.suggestCountries(anyList())).thenReturn(mockJsonResponse);

        mockMvc.perform(post("/api/trip/suggestions/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].country").value("CountryA"))
                .andExpect(jsonPath("$[1].country").value("CountryB"));
    }

    @Test
    public void whenSuggestCountries_aiReturnsErrorJson_thenForwardError() throws Exception {
        DestinationPreferencesRequest preferencesRequest = new DestinationPreferencesRequest();
        preferencesRequest.setPreferences(List.of("beaches"));

        String errorJsonResponseFromAi = "{"error":"AI service unavailable"}";
        when(googleAiService.suggestCountries(anyList())).thenReturn(errorJsonResponseFromAi);

        mockMvc.perform(post("/api/trip/suggestions/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AI service unavailable"));
    }

    @Test
    public void whenSuggestCountries_aiReturnsMalformedJson_thenReturnGenericError() throws Exception {
        DestinationPreferencesRequest preferencesRequest = new DestinationPreferencesRequest();
        preferencesRequest.setPreferences(List.of("mountains"));

        String malformedJsonResponseFromAi = "[{"country":"CountryC", "overview":"Overview C""; 
        when(googleAiService.suggestCountries(anyList())).thenReturn(malformedJsonResponseFromAi);

        mockMvc.perform(post("/api/trip/suggestions/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Invalid AI response format for country suggestions."));
    }

    @Test
    public void whenSuggestCountries_withEmptyPreferences_thenReturnBadRequest() throws Exception {
        DestinationPreferencesRequest preferencesRequest = new DestinationPreferencesRequest();
        preferencesRequest.setPreferences(Collections.emptyList()); 

        mockMvc.perform(post("/api/trip/suggestions/countries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Preferences cannot be empty")));
    }

    // Security Tests for finalizeItinerary
    @Test
    @WithMockUser // Simulates an authenticated user with default username "user" and role "USER"
    public void whenFinalizeItinerary_asAuthenticatedUser_thenReturnsOk() throws Exception {
        Long itineraryId = 1L;
        Itinerary mockFinalizedItinerary = new Itinerary();
        mockFinalizedItinerary.setId(itineraryId);
        mockFinalizedItinerary.setFinalized(true);
        mockFinalizedItinerary.setDestination("Test"); // Add necessary fields for ItineraryResponse
        mockFinalizedItinerary.setNumberOfDays(1);
        mockFinalizedItinerary.setCreatedAt(LocalDateTime.now());

        when(itineraryService.finalizeItinerary(itineraryId)).thenReturn(mockFinalizedItinerary);

        mockMvc.perform(post("/api/trip/{itineraryId}/finalize", itineraryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser // Simulates an anonymous user
    public void whenFinalizeItinerary_asAnonymousUser_thenReturnsUnauthorized() throws Exception {
        Long itineraryId = 1L;

        mockMvc.perform(post("/api/trip/{itineraryId}/finalize", itineraryId)
                .contentType(MediaType.APPLICATION_JSON))
                // For @PreAuthorize, it typically results in 403 Forbidden if authentication is missing for the filter chain,
                // but the AuthEntryPointJwt turns it into 401 if no auth token is found.
                .andExpect(status().isUnauthorized()); 
    }
}
