package com.tripplanner.service;

import com.tripplanner.dto.JwtResponse;
import com.tripplanner.dto.LoginRequest;
import com.tripplanner.dto.SignUpRequest;
import com.tripplanner.entity.User;
import com.tripplanner.exception.AuthenticationException;
import com.tripplanner.exception.UserAlreadyExistsException;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final PasswordEncoder encoder;
  private final JwtUtils jwtUtils;

  public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
      PasswordEncoder encoder, JwtUtils jwtUtils) {

    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.encoder = encoder;
    this.jwtUtils = jwtUtils;
  }

  public JwtResponse authenticateUser(LoginRequest loginRequest) {

    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

      SecurityContextHolder.getContext().setAuthentication(authentication);
      String jwt = jwtUtils.generateJwtToken(authentication);

      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
      logger.info("User {} authenticated successfully", userDetails.getUsername());

      return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail());
    }
    catch (Exception e) {
      logger.error("Authentication failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
      throw new AuthenticationException("Invalid username or password");
    }
  }

  public void registerUser(SignUpRequest signUpRequest) {

    if (Boolean.TRUE.equals(userRepository.existsByUsername(signUpRequest.getUsername()))) {
      logger.warn("Registration failed: Username {} is already taken", signUpRequest.getUsername());
      throw new UserAlreadyExistsException("Username is already taken!");
    }

    if (Boolean.TRUE.equals(userRepository.existsByEmail(signUpRequest.getEmail()))) {
      logger.warn("Registration failed: Email {} is already in use", signUpRequest.getEmail());
      throw new UserAlreadyExistsException("Email is already in use!");
    }

    User user = new User(
        signUpRequest.getUsername(),
        encoder.encode(signUpRequest.getPassword()),
        signUpRequest.getEmail()
    );

    userRepository.save(user);
    logger.info("User {} registered successfully", signUpRequest.getUsername());
  }
}