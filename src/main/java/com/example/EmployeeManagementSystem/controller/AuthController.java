package com.example.EmployeeManagementSystem.controller;


import com.example.EmployeeManagementSystem.dto.request.LoginRequest;
import com.example.EmployeeManagementSystem.dto.response.AuthResponse;
import com.example.EmployeeManagementSystem.security.JwtUtil;
import com.example.EmployeeManagementSystem.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "login", description = "APIs for managing logins")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {

        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (Exception e) {

            throw new BadCredentialsException("Invalid username or password");
        }

        UserDetails userDetails = (UserDetails) authResult.getPrincipal();

        String role = userDetails.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "");

        String token = jwtUtil.generateToken(request.getUsername(), role);

        return ResponseEntity.ok(new AuthResponse(token));
    }
}
