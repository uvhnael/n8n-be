package org.uvhnael.fbadsbe2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/scheduled-posts")
public class ScheduledPostController {

    @PostMapping
    public ResponseEntity<?> schedule(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "scheduled", "request", body));
    }
}
