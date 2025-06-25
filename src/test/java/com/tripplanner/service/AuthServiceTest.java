package com.tripplanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tripplanner.dto.JwtResponse;
import com.tripplanner.dto.LoginRequest;
import com.tripplanner.dto.SignUpRequest;
import com.tripplanner.entity.User;
import com.tripplanner.exception.AuthenticationException;
import com.tripplanner.exception.UserAlreadyExistsException;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.security.JwtUtils;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder encoder;

  @Mock
  private JwtUtils jwtUtils;

  @InjectMocks
  private AuthService authService;

  @BeforeEach
  void setUp() {

    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {

    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticateUser_success() {
    // Arrange
    String username = "testuser";
    String password = "password";
    LoginRequest loginRequest = new LoginRequest(username, password);
    Authentication authentication = mock(Authentication.class);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
    String jwt = "Bearer";
    when(jwtUtils.generateJwtToken(authentication)).thenReturn(jwt);
    Long userId = 1L;
    String email = "test@example.com";
    UserDetailsImpl userDetails = new UserDetailsImpl(userId, username, email, password, Collections.emptyList());
    when(authentication.getPrincipal()).thenReturn(userDetails);

    // Act
    JwtResponse response = authService.authenticateUser(loginRequest);

    // Assert
    assertNotNull(response);
    assertEquals(jwt, response.getTokenType());
    assertEquals(userId, response.getId());
    assertEquals(username, response.getUsername());
    assertEquals(email, response.getEmail());
    assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void authenticateUser_failure() {
    // Arrange
    String username = "testuser";
    String password = "wrongpassword";
    LoginRequest loginRequest = new LoginRequest(username, password);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    // Act & Assert
    assertThrows(AuthenticationException.class, () -> authService.authenticateUser(loginRequest));
  }

  @Test
  void registerUser_success() {
    // Arrange
    String username = "newuser";
    String password = "password";
    String email = "new@example.com";
    SignUpRequest signUpRequest = new SignUpRequest(username, password, email);
    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(false);
    String encodedPassword = "encoded_password";
    when(encoder.encode(password)).thenReturn(encodedPassword);

    // Act
    authService.registerUser(signUpRequest);

    // Assert
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertEquals(username, savedUser.getUsername());
    assertEquals(encodedPassword, savedUser.getPassword());
    assertEquals(email, savedUser.getEmail());
  }

  @Test
  void registerUser_usernameExists() {
    // Arrange
    String username = "existinguser";
    String password = "password";
    String email = "new@example.com";
    SignUpRequest signUpRequest = new SignUpRequest(username, password, email);
    when(userRepository.existsByUsername(username)).thenReturn(true);

    // Act & Assert
    assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(signUpRequest));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void registerUser_emailExists() {
    // Arrange
    String username = "newuser";
    String password = "password";
    String email = "existing@example.com";
    SignUpRequest signUpRequest = new SignUpRequest(username, password, email);
    when(userRepository.existsByUsername(username)).thenReturn(false);
    when(userRepository.existsByEmail(email)).thenReturn(true);

    // Act & Assert
    assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(signUpRequest));
    verify(userRepository, never()).save(any(User.class));
  }
}