package com.tripplanner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignUpRequest {

  @NotBlank
  @Size(min = 3, max = 50)
  private String username;

  @NotBlank
  @Size(min = 6, max = 100)
  private final String password;

  @NotBlank
  @Email
  @Size(max = 100)
  private final String email;

  public SignUpRequest(String username, String password, String email) {

    this.username = username;
    this.password = password;
    this.email = email;
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

  public String getEmail() {

    return email;
  }

}
