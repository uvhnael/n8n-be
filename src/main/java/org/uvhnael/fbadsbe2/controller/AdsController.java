package org.uvhnael.fbadsbe2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ads")
public class AdsController {

    @PostMapping
    public ResponseEntity<?> createAd(@RequestBody Map<String, Object> payload) {
        // placeholder: save ad from n8n
        return ResponseEntity.ok(Map.of("status", "ok", "payload", payload));
    }

    @GetMapping
    public ResponseEntity<List<String>> listAds() {
        return ResponseEntity.ok(List.of());
    }
}
