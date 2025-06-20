package com.tripplanner.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.ai.AiErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private ObjectMapper objectMapper; // For trying to parse AI error responses

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AiErrorDto> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        AiErrorDto errorDto = new AiErrorDto(ex.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AiErrorDto> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        AiErrorDto errorDto = new AiErrorDto(ex.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AiErrorDto> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied: {}", ex.getMessage());
        AiErrorDto errorDto = new AiErrorDto(ex.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<AiErrorDto> handleAccessDeniedException(AiServiceException ex) {
        logger.warn("AI service error: {}", ex.getMessage());
        AiErrorDto errorDto = new AiErrorDto(ex.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AiErrorDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));
        logger.warn("Validation error: {}", errorMessage);
        AiErrorDto errorDto = new AiErrorDto("Validation failed: " + errorMessage);
        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<AiErrorDto> handleJsonProcessingException(JsonProcessingException ex, WebRequest request) {
        // This handler is tricky because the JsonProcessingException might occur when trying to parse
        // an AI response that is *itself* an error message from the AI, or just malformed.
        // The raw response string isn't directly available here unless we wrap the call differently.
        logger.error("JSON Processing Error: {}", ex.getMessage());
        // We can't easily re-parse the original string here as we did in the controller.
        // So, we return a more generic error. The controller-level handling for AI responses is still valuable.
        AiErrorDto errorDto = new AiErrorDto("Error processing JSON response. The format might be invalid.");
        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(IllegalStateException.class) // e.g. modifying finalized itinerary
    public ResponseEntity<AiErrorDto> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        logger.warn("Illegal state: {}", ex.getMessage());
        AiErrorDto errorDto = new AiErrorDto(ex.getMessage());
        return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AiErrorDto> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        AiErrorDto errorDto = new AiErrorDto("An unexpected internal server error occurred. Please try again later.");
        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
