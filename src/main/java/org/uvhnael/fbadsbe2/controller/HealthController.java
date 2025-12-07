package org.uvhnael.fbadsbe2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Health check endpoints for monitoring")
public class HealthController {

    @GetMapping
    @Operation(summary = "Basic health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Facebook Ads Backend");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    @Operation(summary = "Simple ping endpoint")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/status")
    @Operation(summary = "Detailed status check")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Facebook Ads Backend");
        response.put("version", "1.0.0");
        response.put("uptime", "OK");
        
        // Database status (simplified)
        Map<String, String> database = new HashMap<>();
        database.put("status", "UP");
        response.put("database", database);
        
        return ResponseEntity.ok(response);
    }
}
