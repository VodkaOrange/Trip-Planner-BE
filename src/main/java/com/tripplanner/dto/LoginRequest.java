package com.tripplanner.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

  @NotBlank
  private String username;

  @NotBlank
  private final String password;

  public LoginRequest(String username, String password) {

    this.username = username;
    this.password = password;
  }

  public String getUsername() {

    return username;
  }

  public void setUsername(String username) {

    this.username = username;
  }

  public String getPassword() {

    return password;
  }

}
