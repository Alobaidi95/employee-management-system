package com.example.EmployeeManagementSystem.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/users/5");
    }

    @Nested
    class DomainExceptions {

        @Test
        void duplicatedExceptionMapsTo400() {
            ResponseEntity<ApiError> response =
                    handler.handleDuplicated(new DuplicatedException("Username already exists"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Username already exists");
            assertThat(response.getBody().getPath()).isEqualTo("/api/users/5");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }

        @Test
        void resourceNotFoundMapsTo404() {
            ResponseEntity<ApiError> response =
                    handler.handleNotFound(new ResourceNotFoundException("User not found with id: 5"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getStatus()).isEqualTo(404);
        }

        @Test
        void unauthorizedAccessMapsTo403() {
            ResponseEntity<ApiError> response =
                    handler.handleUnauthorized(new UnauthorizedAccessException("Not allowed"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getStatus()).isEqualTo(403);
        }

        @Test
        void unMatchedPasswordsMapsTo400() {
            ResponseEntity<ApiError> response =
                    handler.handleUnmatchedPasswords(new UnMatchedPasswordsException("Old passwords don't match"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void invalidTaskStateMapsTo409() {
            ResponseEntity<ApiError> response =
                    handler.handleInvalidTaskState(new InvalidTaskStateException("Wrong status"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getStatus()).isEqualTo(409);
        }
    }

    @Nested
    class SpringSecurityExceptions {

        @Test
        void authenticationExceptionMapsTo401_withGenericMessage() {
            ResponseEntity<ApiError> response =
                    handler.handleAuthentication(new BadCredentialsException("some internal detail"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

            assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
        }

        @Test
        void accessDeniedMapsTo403_withGenericMessage() {
            ResponseEntity<ApiError> response =
                    handler.handleAccessDenied(new AccessDeniedException("Access is denied"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getMessage()).isEqualTo("You don't have permission to perform this operation");
        }
    }

    @Nested
    class BeanValidation {

        @Test
        void fieldErrorsAreJoinedIntoASingleMessage() {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createUserRequest");
            bindingResult.addError(new FieldError("createUserRequest", "email", "Email is required"));
            bindingResult.addError(new FieldError("createUserRequest", "username", "Username is required"));


            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage())
                    .contains("email: Email is required")
                    .contains("username: Username is required");
        }
    }

    @Nested
    class CatchAll {

        @Test
        void unexpectedExceptionMapsTo500_withoutLeakingDetails() {
            ResponseEntity<ApiError> response =
                    handler.handleGeneric(new RuntimeException("some stack-trace-worthy internal failure"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        }
    }
}