package com.tripplanner.controller;

import com.tripplanner.dto.JwtResponse;
import com.tripplanner.dto.LoginRequest;
import com.tripplanner.dto.SignUpRequest;
import com.tripplanner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {

    this.authService = authService;
  }

  @PostMapping("/signin")
  public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
    return ResponseEntity.ok(jwtResponse);
  }

  @PostMapping("/signup")
  public ResponseEntity<String> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {

    authService.registerUser(signUpRequest);
    return ResponseEntity.ok("User registered successfully");
  }
}