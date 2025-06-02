package com.tripplanner.dto.ai;

// DTO for representing an error response from the AI service or during its processing
public class AiErrorDto {
    private String error;

    public AiErrorDto() {}

    public AiErrorDto(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
