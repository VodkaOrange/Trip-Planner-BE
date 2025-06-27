package com.tripplanner.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAiConfig {

  private static final Logger log = LoggerFactory.getLogger(GoogleAiConfig.class);
  @Value("${google.ai.project-id}")
  private String projectId;

  @Value("${google.ai.location}")
  private String location;

  @Value("${google.ai.application.credentials}")
  private String credentialsPath;

  @Bean
  public VertexAI vertexAI() throws IOException {
    log.info("Attempting to load credentials from: {}", credentialsPath);
    if (credentialsPath == null || credentialsPath.isEmpty()) {
      throw new IllegalStateException("Google app credentials environment variable is not set");
    }
    try {
      GoogleCredentials credentials = GoogleCredentials.fromStream(
          new FileInputStream(credentialsPath)
      ).createScoped("https://www.googleapis.com/auth/cloud-platform");
      return new VertexAI.Builder()
          .setProjectId(projectId)
          .setLocation(location)
          .setCredentials(credentials)
          .build();
    } catch (IOException e) {
      throw new IOException("Failed to load credentials: " + e.getMessage());
    }
  }
}