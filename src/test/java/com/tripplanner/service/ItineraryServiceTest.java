package com.tripplanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripplanner.entity.Itinerary;
import com.tripplanner.entity.User;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.ItineraryRepository;
import com.tripplanner.repository.UserRepository;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

  @Mock
  private ItineraryRepository itineraryRepository;

  @Mock
  private UserRepository userRepository;

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

  // Tests for finalizeItinerary
  @Test
  void whenFinalizeItinerary_unownedItinerary_byAuthenticatedUser_thenClaimedAndFinalized() {

    Long itineraryId = 1L;
    Long userId = 5L;
    mockAuthenticatedUser(userId, "claimer", true);

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
  void whenFinalizeItinerary_ownedByCurrentUser_thenFinalized() {

    Long itineraryId = 2L;
    Long userId = 6L;
    mockAuthenticatedUser(userId, "owner", false);

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
  void whenFinalizeItinerary_ownedByAnotherUser_thenThrowAccessDenied() {

    Long itineraryId = 3L;
    Long ownerUserId = 7L;
    Long currentUserId = 8L;

    mockAuthenticatedUser(currentUserId, "currentUser", false);

    User actualOwner = new User();
    actualOwner.setId(ownerUserId);

    Itinerary otherUserItinerary = new Itinerary();
    otherUserItinerary.setId(itineraryId);
    otherUserItinerary.setUser(actualOwner);

    when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(otherUserItinerary));

    assertThrows(AccessDeniedException.class, () -> itineraryService.finalizeItinerary(itineraryId));
    verify(itineraryRepository, never()).save(any(Itinerary.class));
  }

  @Test
  void whenFinalizeItinerary_byAnonymousUser_thenThrowAccessDenied() {

    Long itineraryId = 4L;
    mockAnonymousUser(); // Set up anonymous user context

    Itinerary someItinerary = new Itinerary(); // Content doesn't matter much here
    someItinerary.setId(itineraryId);
    when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(someItinerary));

    assertThrows(AccessDeniedException.class, () -> itineraryService.finalizeItinerary(itineraryId));
    verify(itineraryRepository, never()).save(any(Itinerary.class));
  }

  @Test
  void whenFinalizeItinerary_itineraryAlreadyFinalized_thenReturnAsIs() {

    Long itineraryId = 5L;
    Long userId = 9L;
    mockAuthenticatedUser(userId, "finalizer", false);

    User owner = new User();
    owner.setId(userId);

    Itinerary alreadyFinalizedItinerary = new Itinerary();
    alreadyFinalizedItinerary.setId(itineraryId);
    alreadyFinalizedItinerary.setUser(owner);
    alreadyFinalizedItinerary.setFinalized(true);
    alreadyFinalizedItinerary.setShareableLink("existing-link");

    when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.of(alreadyFinalizedItinerary));

    Itinerary result = itineraryService.finalizeItinerary(itineraryId);

    assertSame(alreadyFinalizedItinerary, result);
    assertTrue(result.isFinalized());
    assertEquals("existing-link", result.getShareableLink());
    verify(itineraryRepository, never()).save(any(Itinerary.class));
  }

  @Test
  void whenFinalizeItinerary_itineraryNotFound_thenThrowResourceNotFound() {

    Long itineraryId = 6L;
    when(itineraryRepository.findById(itineraryId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> itineraryService.finalizeItinerary(itineraryId));
  }

  private void mockAuthenticatedUser(Long userId, String username, boolean stubUserRepository) {

    User mockUser = new User(username, "password", username + "@example.com");
    mockUser.setId(userId);
    UserDetailsImpl userDetails = UserDetailsImpl.build(mockUser);
    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(securityContext);
    if (stubUserRepository) {
      when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
    }
  }

  private void mockAnonymousUser() {

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true); // Or false depending on how anonymous is set up
    when(auth.getPrincipal()).thenReturn("anonymousUser");
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(securityContext);
  }
}
