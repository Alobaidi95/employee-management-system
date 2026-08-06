package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.dto.request.LoginRequest;
import com.example.EmployeeManagementSystem.dto.response.AuthResponse;
import com.example.EmployeeManagementSystem.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authResult;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authenticationManager, jwtUtil);
    }

    @Test
    void successfulLoginReturnsAToken() {
        LoginRequest request = new LoginRequest("john", "correct-password");

        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("john")
                .password("hashed")
                .authorities("ROLE_EMPLOYEE")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authResult);
        when(authResult.getPrincipal()).thenReturn(principal);
        when(jwtUtil.generateToken("john", "EMPLOYEE")).thenReturn("fake.jwt.token");

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isEqualTo("fake.jwt.token");
    }

    @Test
    void stripsTheRolePrefixBeforeGeneratingTheToken() {
        LoginRequest request = new LoginRequest("admin", "adminpass");

        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("admin")
                .password("hashed")
                .authorities("ROLE_ADMIN")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authResult);
        when(authResult.getPrincipal()).thenReturn(principal);
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("admin.jwt.token");

        authController.login(request);


        verify(jwtUtil).generateToken("admin", "ADMIN");
    }

    @Test
    void badCredentialsAreWrappedIntoACleanGenericException() {
        LoginRequest request = new LoginRequest("john", "wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));


        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    void anyAuthenticationFailure_notJustBadCredentials_stillMapsToTheSameCleanException() {
        LoginRequest request = new LoginRequest("john", "whatever");


        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("some unrelated internal failure"));

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}