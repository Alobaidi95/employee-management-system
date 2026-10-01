package com.example.EmployeeManagementSystem.controller;

import com.example.EmployeeManagementSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());

        try {
            long userCount = userRepository.count();
            body.put("database", "UP");
            body.put("userCount", userCount);
        } catch (Exception ex) {
            log.error("Health check DB query failed", ex);
            body.put("database", "DOWN");
            body.put("error", ex.getMessage());
        }

        return ResponseEntity.ok(body);
    }
}