package com.sprintdesk.exception;

import com.sprintdesk.api.ApiError;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<Map<String, String>> fields =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::fieldError)
            .collect(Collectors.toList());
    Map<String, Object> details = new HashMap<>();
    details.put("fields", fields);
    return ResponseEntity.badRequest().body(new ApiError("Validation failed", "validation_error", details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
    Map<String, Object> details = new HashMap<>();
    details.put("violations", ex.getConstraintViolations());
    return ResponseEntity.badRequest().body(new ApiError("Validation failed", "validation_error", details));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest().body(new ApiError("Malformed request", "invalid_request", null));
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ApiError> handleErrorResponse(ErrorResponseException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    String message = ex.getMessage();
    if (message == null || message.isBlank()) {
      message = status.getReasonPhrase();
    }
    return ResponseEntity.status(status).body(new ApiError(message, "request_failed", null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError("Unexpected error", "internal_error", null));
  }

  private Map<String, String> fieldError(FieldError fieldError) {
    Map<String, String> result = new HashMap<>();
    result.put("field", fieldError.getField());
    result.put("message", fieldError.getDefaultMessage());
    return result;
  }
}
