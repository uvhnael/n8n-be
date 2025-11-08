package org.uvhnael.fbadsbe2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    @GetMapping("/latest")
    public ResponseEntity<?> latest() {
        return ResponseEntity.ok(Map.of("message", "latest insight"));
    }
}
