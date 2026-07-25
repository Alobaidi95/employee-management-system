package com.example.EmployeeManagementSystem.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ApiError buildError(HttpStatus status, String message, HttpServletRequest request) {
        return ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }

    // ----  custom domain exceptions ----

    @ExceptionHandler(DuplicatedException.class)
    public ResponseEntity<ApiError> handleDuplicated(DuplicatedException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedAccessException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(UnMatchedPasswordsException.class)
    public ResponseEntity<ApiError> handleUnmatchedPasswords(UnMatchedPasswordsException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ---- Spring Security exceptions ----

    // Thrown by authenticationManager.authenticate() on bad login credentials
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // Thrown when a @PreAuthorize check fails (e.g. non-ADMIN calling an ADMIN-only method)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.FORBIDDEN, "You don't have permission to perform this operation", request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ---- Bean validation (@Valid on @RequestBody DTOs) ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ApiError body = buildError(HttpStatus.BAD_REQUEST, message, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ---- Catch-all safety net ----
    // Anything not explicitly handled above lands here as a clean 500
    // instead of leaking a raw stack trace to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        ApiError body = buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}