package com.tripplanner.config;

import com.google.cloud.vertexai.VertexAI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class GoogleAiConfig {

    @Value("${google.ai.project-id}")
    private String projectId;

    @Value("${google.ai.location}")
    private String location;

    @Bean
    public VertexAI vertexAI() throws IOException {
        // Initializes a VertexAI client with Application Default Credentials (ADC)
        // Authentication via `gcloud auth application-default login` required in advance
        return new VertexAI(projectId, location);
    }
}
