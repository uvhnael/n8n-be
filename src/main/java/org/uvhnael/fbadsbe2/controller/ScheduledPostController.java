package org.uvhnael.fbadsbe2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uvhnael.fbadsbe2.model.dto.ScheduledPostDTO;
import org.uvhnael.fbadsbe2.service.PostPublisherService;

import java.util.Map;

@RestController
@RequestMapping("/api/scheduled-posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scheduled Posts", description = "APIs for scheduling posts via n8n webhook")
public class ScheduledPostController {

    private final PostPublisherService postPublisherService;

    @PostMapping
    @Operation(summary = "Schedule a post via n8n webhook")
    public ResponseEntity<?> schedulePost(@RequestBody ScheduledPostDTO dto) {
        try {
            postPublisherService.schedulePostViaN8n(dto);
            return ResponseEntity.ok(Map.of("message", "Post scheduled successfully via n8n"));
        } catch (Exception e) {
            log.error("Error scheduling post via n8n: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/publish-now")
    @Operation(summary = "Publish a post immediately via n8n webhook")
    public ResponseEntity<?> publishNow(@RequestBody ScheduledPostDTO dto) {
        try {
            // For immediate publishing, set scheduled time to now (ISO format)
            dto.setScheduledTime(java.time.OffsetDateTime.now().toString());
            postPublisherService.schedulePostViaN8n(dto);
            return ResponseEntity.ok(Map.of("message", "Post published immediately via n8n"));
        } catch (Exception e) {
            log.error("Error publishing post immediately via n8n: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
