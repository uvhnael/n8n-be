package org.uvhnael.fbadsbe2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uvhnael.fbadsbe2.model.dto.ContentGenerateRequest;
import org.uvhnael.fbadsbe2.model.entity.GeneratedContent;
import org.uvhnael.fbadsbe2.repository.GeneratedContentRepository;
import org.uvhnael.fbadsbe2.service.ContentGeneratorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Content Generator", description = "APIs for AI-powered content generation")
public class ContentController {

    private final ContentGeneratorService contentGeneratorService;
    private final GeneratedContentRepository contentRepository;

    @PostMapping("/generate")
    @Operation(summary = "Generate new content using AI")
    public ResponseEntity<?> generateContent(@RequestBody ContentGenerateRequest request) {
        try {
            GeneratedContent content = contentGeneratorService.generateContent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(content);
        } catch (Exception e) {
            log.error("Error generating content: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all generated content")
    public ResponseEntity<List<GeneratedContent>> getAllContent(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String platform) {
        
        List<GeneratedContent> contents = contentRepository.findAll();
        
        // Apply filters if provided
        if (status != null) {
            contents = contents.stream()
                .filter(c -> status.equalsIgnoreCase(c.getStatus()))
                .toList();
        }
        if (contentType != null) {
            contents = contents.stream()
                .filter(c -> contentType.equalsIgnoreCase(c.getContentType()))
                .toList();
        }
        if (platform != null) {
            contents = contents.stream()
                .filter(c -> platform.equalsIgnoreCase(c.getPlatform()))
                .toList();
        }
        
        return ResponseEntity.ok(contents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get content by ID")
    public ResponseEntity<?> getContentById(@PathVariable Long id) {
        return contentRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve content for publishing")
    public ResponseEntity<?> approveContent(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Long approvedBy) {
        try {
            GeneratedContent approved = contentGeneratorService.approveContent(id, approvedBy);
            return ResponseEntity.ok(Map.of(
                "message", "Content approved successfully",
                "content", approved
            ));
        } catch (Exception e) {
            log.error("Error approving content: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject content")
    public ResponseEntity<?> rejectContent(@PathVariable Long id) {
        try {
            GeneratedContent rejected = contentGeneratorService.rejectContent(id);
            return ResponseEntity.ok(Map.of(
                "message", "Content rejected",
                "content", rejected
            ));
        } catch (Exception e) {
            log.error("Error rejecting content: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete generated content")
    public ResponseEntity<?> deleteContent(@PathVariable Long id) {
        try {
            contentRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Content deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting content: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-batch")
    @Operation(summary = "Generate multiple content pieces at once")
    public ResponseEntity<?> generateBatch(@RequestBody Map<String, Object> request) {
        try {
            int count = (int) request.getOrDefault("count", 3);
            
            // Build base request
            ContentGenerateRequest baseRequest = new ContentGenerateRequest();
            baseRequest.setContentType((String) request.getOrDefault("contentType", "POST"));
            baseRequest.setPlatform((String) request.getOrDefault("platform", "FACEBOOK"));
            baseRequest.setTone((String) request.getOrDefault("tone", "FRIENDLY"));
            baseRequest.setLength((String) request.getOrDefault("length", "MEDIUM"));
            baseRequest.setIncludeHashtags((Boolean) request.getOrDefault("includeHashtags", true));
            baseRequest.setIncludeCTA((Boolean) request.getOrDefault("includeCTA", true));
            
            List<GeneratedContent> contents = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                try {
                    GeneratedContent content = contentGeneratorService.generateContent(baseRequest);
                    contents.add(content);
                    
                    // Add small delay between generations
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("Error generating content #{}: {}", i + 1, e.getMessage());
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Generated " + contents.size() + " content pieces",
                "contents", contents
            ));
            
        } catch (Exception e) {
            log.error("Error in batch generation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get content suggestions based on current trends")
    public ResponseEntity<?> getContentSuggestions() {
        // This would typically call TrendAnalysisService
        // For now, return placeholder
        List<Map<String, Object>> suggestions = List.of(
            Map.of(
                "topic", "Ưu đãi spa cuối tuần",
                "keywords", List.of("giảm giá", "spa", "cuối tuần"),
                "trendScore", 92,
                "suggestedPostingTime", "2024-01-19T14:00:00"
            ),
            Map.of(
                "topic", "Chăm sóc da mùa đông",
                "keywords", List.of("chăm sóc da", "mùa đông", "làm đẹp"),
                "trendScore", 85,
                "suggestedPostingTime", "2024-01-20T10:00:00"
            )
        );
        
        return ResponseEntity.ok(Map.of(
            "suggestions", suggestions,
            "message", "Based on current trends and insights"
        ));
    }
}
