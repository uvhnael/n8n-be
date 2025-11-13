package org.uvhnael.fbadsbe2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uvhnael.fbadsbe2.model.dto.ScheduledPostDTO;
import org.uvhnael.fbadsbe2.model.entity.ScheduledPost;
import org.uvhnael.fbadsbe2.service.PostPublisherService;
import org.uvhnael.fbadsbe2.service.ScheduledPostService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled-posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scheduled Posts", description = "APIs for scheduling and managing posts")
public class ScheduledPostController {

    private final ScheduledPostService scheduledPostService;
    private final PostPublisherService postPublisherService;

    @PostMapping
    @Operation(summary = "Schedule a new post")
    public ResponseEntity<?> schedulePost(@RequestBody ScheduledPostDTO dto) {
        try {
            ScheduledPost post = scheduledPostService.schedulePost(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(post);
        } catch (Exception e) {
            log.error("Error scheduling post: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all scheduled posts")
    public ResponseEntity<List<ScheduledPost>> getAllScheduledPosts() {
        List<ScheduledPost> posts = scheduledPostService.getAllScheduledPosts();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a scheduled post by ID")
    public ResponseEntity<?> getScheduledPostById(@PathVariable Long id) {
        try {
            ScheduledPost post = scheduledPostService.getScheduledPostById(id);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a scheduled post")
    public ResponseEntity<?> updateScheduledPost(
            @PathVariable Long id,
            @RequestBody ScheduledPostDTO dto) {
        try {
            ScheduledPost updated = scheduledPostService.updateScheduledPost(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating post: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a scheduled post")
    public ResponseEntity<?> cancelScheduledPost(@PathVariable Long id) {
        try {
            scheduledPostService.cancelScheduledPost(id);
            return ResponseEntity.ok(Map.of("message", "Post cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling post: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    @Operation(summary = "Schedule multiple posts at once")
    public ResponseEntity<?> scheduleBulk(@RequestBody List<ScheduledPostDTO> dtos) {
        try {
            List<ScheduledPost> posts = dtos.stream()
                .map(scheduledPostService::schedulePost)
                .toList();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Scheduled " + posts.size() + " posts successfully",
                "posts", posts
            ));
        } catch (Exception e) {
            log.error("Error scheduling bulk posts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming posts to be published")
    public ResponseEntity<List<ScheduledPost>> getUpcomingPosts() {
        List<ScheduledPost> posts = scheduledPostService.getPostsToPublish();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get scheduled posts in calendar format")
    public ResponseEntity<?> getCalendar() {
        List<ScheduledPost> posts = scheduledPostService.getAllScheduledPosts();
        
        // Group by date
        Map<String, List<ScheduledPost>> calendar = new HashMap<>();
        for (ScheduledPost post : posts) {
            String date = post.getScheduledTime().toLocalDate().toString();
            calendar.computeIfAbsent(date, k -> new java.util.ArrayList<>()).add(post);
        }
        
        return ResponseEntity.ok(calendar);
    }

    @PostMapping("/{id}/publish-now")
    @Operation(summary = "Publish a post immediately (bypass schedule)")
    public ResponseEntity<?> publishNow(@PathVariable Long id) {
        try {
            postPublisherService.publishNow(id);
            return ResponseEntity.ok(Map.of("message", "Post published successfully"));
        } catch (Exception e) {
            log.error("Error publishing post: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule a post to a new time")
    public ResponseEntity<?> reschedule(
            @PathVariable Long id,
            @RequestBody ScheduledPostDTO dto) {
        try {
            ScheduledPost updated = scheduledPostService.updateScheduledPost(id, dto);
            return ResponseEntity.ok(Map.of(
                "message", "Post rescheduled successfully",
                "post", updated
            ));
        } catch (Exception e) {
            log.error("Error rescheduling post: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
