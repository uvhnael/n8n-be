package org.uvhnael.fbadsbe2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uvhnael.fbadsbe2.model.entity.TrendAnalysis;
import org.uvhnael.fbadsbe2.service.TrendAnalysisService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trend Analysis", description = "APIs for analyzing and retrieving trending content and keywords")
public class TrendAnalysisController {

    private final TrendAnalysisService trendAnalysisService;

    /**
     * Get current trends
     */
    @GetMapping("/current")
    @Operation(summary = "Get current trends", description = "Get the most recent trend analysis")
    public ResponseEntity<?> getCurrentTrends() {
        try {
            TrendAnalysis trend = trendAnalysisService.getLatestTrend();
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            log.error("Error getting current trends: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all trends
     */
    @GetMapping
    @Operation(summary = "List all trends", description = "Get all trend analyses")
    public ResponseEntity<List<TrendAnalysis>> getAllTrends() {
        List<TrendAnalysis> trends = trendAnalysisService.getAllTrends();
        return ResponseEntity.ok(trends);
    }

    /**
     * Get trending keywords
     */
    @GetMapping("/keywords")
    @Operation(summary = "Get trending keywords", description = "Get trending keywords from the latest trend analysis")
    public ResponseEntity<?> getTrendingKeywords() {
        try {
            TrendAnalysis trend = trendAnalysisService.getLatestTrend();
            return ResponseEntity.ok(Map.of(
                "trendingKeywords", trend.getTrendingKeywords(),
                "analysisDate", trend.getAnalysisDate()
            ));
        } catch (Exception e) {
            log.error("Error getting trending keywords: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get content suggestions
     */
    @GetMapping("/suggestions")
    @Operation(summary = "Get content suggestions", description = "Get AI-generated content suggestions from the latest trend analysis")
    public ResponseEntity<?> getContentSuggestions() {
        try {
            TrendAnalysis trend = trendAnalysisService.getLatestTrend();
            return ResponseEntity.ok(Map.of(
                "contentSuggestions", trend.getContentSuggestions(),
                "analysisDate", trend.getAnalysisDate()
            ));
        } catch (Exception e) {
            log.error("Error getting content suggestions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Force analyze trends now
     */
    @PostMapping("/analyze")
    @Operation(summary = "Analyze trends now", description = "Trigger immediate trend analysis (normally runs daily at 6 AM)")
    public ResponseEntity<?> analyzeTrendsNow() {
        try {
            log.info("Manual trend analysis triggered");
            TrendAnalysis result = trendAnalysisService.analyzeWeeklyTrends();
            return ResponseEntity.ok(Map.of(
                "message", "Trend analysis completed successfully",
                "analysisId", result.getId(),
                "analysisDate", result.getAnalysisDate()
            ));
        } catch (Exception e) {
            log.error("Error analyzing trends: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get trend by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get trend by ID", description = "Retrieve a specific trend analysis by its ID")
    public ResponseEntity<?> getTrendById(@PathVariable Long id) {
        try {
            TrendAnalysis trend = trendAnalysisService.getTrendById(id);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            log.error("Error getting trend: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
