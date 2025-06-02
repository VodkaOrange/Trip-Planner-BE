package com.tripplanner.service;

import com.tripplanner.dto.AddActivityRequest;
import com.tripplanner.dto.ItineraryRequest;
import com.tripplanner.dto.UpdateInterestsRequest;
import com.tripplanner.entity.*;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException; 

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ItineraryService {

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private DayPlanRepository dayPlanRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Transactional
    public Itinerary createItinerary(ItineraryRequest itineraryRequest) {
        if (!itineraryRequest.isTermsAccepted()) {
            throw new IllegalArgumentException("Terms and conditions must be accepted to create an itinerary.");
        }

        Itinerary itinerary = new Itinerary();
        itinerary.setDestination(itineraryRequest.getDestination());
        itinerary.setNumberOfDays(itineraryRequest.getNumberOfDays());
        itinerary.setBudgetRange(itineraryRequest.getBudgetRange());
        itinerary.setTermsAccepted(true);
        itinerary.setFinalized(false); 

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Optional<User> userOptional = userRepository.findById(userDetails.getId());
            userOptional.ifPresent(itinerary::setUser);
        }

        for (int i = 1; i <= itineraryRequest.getNumberOfDays(); i++) {
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
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));
        
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

    public List<Interest> getAllInterests() {
        return interestRepository.findAll();
    }

    @Transactional
    public Itinerary addActivityToDay(Long itineraryId, int dayNumber, AddActivityRequest activityRequest) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        if (itinerary.isFinalized()) {
            throw new IllegalStateException("Cannot add activities to a finalized itinerary.");
        }

        if (dayNumber <= 0 || dayNumber > itinerary.getNumberOfDays()) {
            throw new IllegalArgumentException("Invalid day number: " + dayNumber +
                                             " for an itinerary with " + itinerary.getNumberOfDays() + " days.");
        }

        DayPlan dayPlan = itinerary.getDayPlans().stream()
                .filter(dp -> dp.getDayNumber() == dayNumber)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("DayPlan not found for day number: " + dayNumber + " in itinerary id: " + itineraryId));

        Activity activity = new Activity(
                activityRequest.getName(),
                activityRequest.getCity(),
                activityRequest.getDescription(),
                activityRequest.getExpectedDurationHours(),
                activityRequest.getEstimatedCostEUR()
        );

        dayPlan.addActivity(activity);
        dayPlanRepository.save(dayPlan);

        return itineraryRepository.findById(itineraryId).orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId)); 
    }

    @Transactional
    public Itinerary finalizeItinerary(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary", "id", itineraryId));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || (authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            throw new AccessDeniedException("User must be logged in to finalize an itinerary.");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();

        if (itinerary.getUser() == null) {
            // Itinerary is unowned (created anonymously), claim it for the current user.
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId)); // Should not happen if JWT is valid
            itinerary.setUser(currentUser);
        } else if (!itinerary.getUser().getId().equals(currentUserId)) {
            // Itinerary is owned by someone else.
            throw new AccessDeniedException("User not authorized to finalize this itinerary. It is owned by another user.");
        }
        // If itinerary.getUser().getId().equals(currentUserId), user is already the owner, proceed.

        if (itinerary.isFinalized()) {
            return itinerary; // Already finalized, no changes needed.
        }

        itinerary.setFinalized(true);
        if (itinerary.getShareableLink() == null || itinerary.getShareableLink().isEmpty()) {
            itinerary.setShareableLink(UUID.randomUUID().toString());
        }
        
        return itineraryRepository.save(itinerary);
    }
}
