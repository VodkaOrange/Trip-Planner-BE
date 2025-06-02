package com.tripplanner.service;

import com.tripplanner.dto.ItineraryRequest;
import com.tripplanner.entity.Itinerary;
import com.tripplanner.entity.User;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.DayPlanRepository;
import com.tripplanner.repository.ItineraryRepository;
import com.tripplanner.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) 
public class ItineraryServiceTest {

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private UserRepository userRepository;
    
    @Mock 
    private DayPlanRepository dayPlanRepository; 

    @InjectMocks 
    private ItineraryService itineraryService;
    
    private SecurityContext originalSecurityContext;

    @BeforeEach
    void setUpContext() {
        // Store original context
        originalSecurityContext = SecurityContextHolder.getContext();
    }

    @AfterEach
    void tearDownContext() {
        // Restore original context
        SecurityContextHolder.setContext(originalSecurityContext);
    }

    private void mockAuthenticatedUser(Long userId, String username) {
        User mockUser = new User(username, "password", username + "@example.com");
        mockUser.setId(userId);
        UserDetailsImpl userDetails = UserDetailsImpl.build(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser)); // Ensure user repo returns this user
    }

    private void mockAnonymousUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true); // Or false depending on how anonymous is set up
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void whenCreateItinerary_withValidRequest_andAnonymousUser_thenItineraryCreatedWithoutUser() {
        ItineraryRequest request = new ItineraryRequest();
        request.setDestination("Test Destination");
        request.setNumberOfDays(3);
        request.setTermsAccepted(true);
        request.setBudgetRange("100-200");

        mockAnonymousUser();

        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(invocation -> {
            Itinerary itinerary = invocation.getArgument(0);
            itinerary.setId(1L); 
            return itinerary;
        });

        Itinerary result = itineraryService.createItinerary(request);

        assertNotNull(result);
        assertEquals("Test Destination", result.getDestination());
        assertNull(result.getUser()); 
        verify(itineraryRepository, times(1)).save(any(Itinerary.class));
        verify(userRepository, never()).findById(anyLong()); 
    }

    @Test
    public void whenCreateItinerary_withValidRequest_andAuthenticatedUser_thenItineraryCreatedWithUser() {
        ItineraryRequest request = new ItineraryRequest();
        request.setDestination("User Destination");
        request.setNumberOfDays(2);
        request.setTermsAccepted(true);

        mockAuthenticatedUser(10L, "testuser");

        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Itinerary result = itineraryService.createItinerary(request);

        assertNotNull(result);
        assertEquals("User Destination", result.getDestination());
        assertNotNull(result.getUser());
        assertEquals(10L, result.getUser().getId());
        verify(itineraryRepository, times(1)).save(any(Itinerary.class));
    }

    @Test
    public void whenCreateItinerary_withTermsNotAccepted_thenThrowIllegalArgumentException() {
        ItineraryRequest request = new ItineraryRequest();
        request.setTermsAccepted(false);
        assertThrows(IllegalArgumentException.class, () -> itineraryService.createItinerary(request));
        verify(itineraryRepository, never()).save(any(Itinerary.class));
    }

    // Tests for finalizeItinerary
    @Test
    public void whenFinalizeItinerary_unownedItinerary_byAuthenticatedUser_thenClaimedAndFinalized() {
        Long itineraryId = 1L;
        Long userId = 5L;
        mockAuthenticatedUser(userId, "claimer");

        Itinerary unownedItinerary = new Itinerary(); // User is null
        unownedItinerary.setId(itineraryId);
        unownedItinerary.setFinalized(false);

        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(unownedItinerary));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // userRepository.findById(userId) is already mocked in mockAuthenticatedUser

        Itinerary result = itineraryService.finalizeItinerary(itineraryId);

        assertTrue(result.isFinalized());
        assertNotNull(result.getShareableLink());
        assertNotNull(result.getUser());
        assertEquals(userId, result.getUser().getId());
        verify(itineraryRepository).save(unownedItinerary); // ensure it's the same instance that gets user set and saved
    }

    @Test
    public void whenFinalizeItinerary_ownedByCurrentUser_thenFinalized() {
        Long itineraryId = 2L;
        Long userId = 6L;
        mockAuthenticatedUser(userId, "owner");

        User owner = new User();
        owner.setId(userId);

        Itinerary ownedItinerary = new Itinerary();
        ownedItinerary.setId(itineraryId);
        ownedItinerary.setUser(owner);
        ownedItinerary.setFinalized(false);

        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(ownedItinerary));
        when(itineraryRepository.save(any(Itinerary.class))).thenReturn(ownedItinerary);

        Itinerary result = itineraryService.finalizeItinerary(itineraryId);

        assertTrue(result.isFinalized());
        assertNotNull(result.getShareableLink());
        assertEquals(userId, result.getUser().getId());
        verify(itineraryRepository).save(ownedItinerary);
    }

    @Test
    public void whenFinalizeItinerary_ownedByAnotherUser_thenThrowAccessDenied() {
        Long itineraryId = 3L;
        Long ownerUserId = 7L;
        Long currentUserId = 8L;

        mockAuthenticatedUser(currentUserId, "currentUser"); // Current user is 8

        User actualOwner = new User();
        actualOwner.setId(ownerUserId); // Itinerary owned by user 7

        Itinerary otherUserItinerary = new Itinerary();
        otherUserItinerary.setId(itineraryId);
        otherUserItinerary.setUser(actualOwner);

        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(otherUserItinerary));

        assertThrows(AccessDeniedException.class, () -> {
            itineraryService.finalizeItinerary(itineraryId);
        });
        verify(itineraryRepository, never()).save(any(Itinerary.class));
    }

    @Test
    public void whenFinalizeItinerary_byAnonymousUser_thenThrowAccessDenied() {
        Long itineraryId = 4L;
        mockAnonymousUser(); // Set up anonymous user context

        Itinerary someItinerary = new Itinerary(); // Content doesn't matter much here
        someItinerary.setId(itineraryId);
        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(someItinerary));
        
        assertThrows(AccessDeniedException.class, () -> {
            itineraryService.finalizeItinerary(itineraryId);
        });
         verify(itineraryRepository, never()).save(any(Itinerary.class));
    }

    @Test
    public void whenFinalizeItinerary_itineraryAlreadyFinalized_thenReturnAsIs() {
        Long itineraryId = 5L;
        Long userId = 9L;
        mockAuthenticatedUser(userId, "finalizer");

        User owner = new User();
        owner.setId(userId);

        Itinerary alreadyFinalizedItinerary = new Itinerary();
        alreadyFinalizedItinerary.setId(itineraryId);
        alreadyFinalizedItinerary.setUser(owner);
        alreadyFinalizedItinerary.setFinalized(true);
        alreadyFinalizedItinerary.setShareableLink("existing-link");

        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(alreadyFinalizedItinerary));

        Itinerary result = itineraryService.finalizeItinerary(itineraryId);

        assertSame(alreadyFinalizedItinerary, result); // Should return the same instance
        assertTrue(result.isFinalized());
        assertEquals("existing-link", result.getShareableLink());
        verify(itineraryRepository, never()).save(any(Itinerary.class)); // No save if already finalized
    }
    
    @Test
    public void whenFinalizeItinerary_itineraryNotFound_thenThrowResourceNotFound() {
        Long itineraryId = 6L;
        mockAuthenticatedUser(1L, "anyUser"); // User must be authenticated

        when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            itineraryService.finalizeItinerary(itineraryId);
        });
    }
}
