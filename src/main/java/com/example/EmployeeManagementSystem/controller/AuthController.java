package com.example.EmployeeManagementSystem.controller;


import com.example.EmployeeManagementSystem.dto.request.LoginRequest;
import com.example.EmployeeManagementSystem.dto.response.AuthResponse;
import com.example.EmployeeManagementSystem.security.JwtUtil;
import com.example.EmployeeManagementSystem.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        String role = userDetails.getAuthorities().iterator().next().getAuthority()
                .replace("ROLE_", "");

        String token = jwtUtil.generateToken(
                request.getUsername(), role
        );

        return ResponseEntity.ok(new AuthResponse(token));
    }
}
