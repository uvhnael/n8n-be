package org.uvhnael.fbadsbe2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uvhnael.fbadsbe2.model.entity.Insight;
import org.uvhnael.fbadsbe2.model.entity.Keyword;
import org.uvhnael.fbadsbe2.service.InsightsService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Insights & Analytics", description = "APIs for generating and retrieving insights from ads data")
public class InsightsController {

    private final InsightsService insightsService;

    /**
     * Generate insight from ads data
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate insight", description = "Generate insights from ads data for a specific date range")
    public ResponseEntity<?> generateInsight(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Insight insight = insightsService.generateInsight(startDate, endDate);
            return ResponseEntity.status(HttpStatus.CREATED).body(insight);
        } catch (Exception e) {
            log.error("Error generating insight: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate insight for current week
     */
    @PostMapping("/generate/current-week")
    @Operation(summary = "Generate current week insight", description = "Generate insights for the current week (Monday to Sunday)")
    public ResponseEntity<?> generateCurrentWeekInsight() {
        try {
            Insight insight = insightsService.generateCurrentWeekInsight();
            return ResponseEntity.status(HttpStatus.CREATED).body(insight);
        } catch (Exception e) {
            log.error("Error generating current week insight: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get latest insight
     */
    @GetMapping("/latest")
    @Operation(summary = "Get latest insight", description = "Get the most recently generated insight")
    public ResponseEntity<?> getLatestInsight() {
        try {
            Insight insight = insightsService.getLatestInsight();
            return ResponseEntity.ok(insight);
        } catch (Exception e) {
            log.error("Error getting latest insight: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all insights
     */
    @GetMapping
    @Operation(summary = "List all insights", description = "Get all generated insights")
    public ResponseEntity<List<Insight>> getAllInsights() {
        List<Insight> insights = insightsService.getAllInsights();
        return ResponseEntity.ok(insights);
    }

    /**
     * Get insight by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get insight by ID", description = "Retrieve a specific insight by its ID")
    public ResponseEntity<?> getInsightById(@PathVariable Long id) {
        try {
            Insight insight = insightsService.getInsightById(id);
            return ResponseEntity.ok(insight);
        } catch (Exception e) {
            log.error("Error getting insight: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get keywords for an insight
     */
    @GetMapping("/{id}/keywords")
    @Operation(summary = "Get insight keywords", description = "Get all keywords associated with a specific insight")
    public ResponseEntity<?> getInsightKeywords(@PathVariable Long id) {
        try {
            List<Keyword> keywords = insightsService.getKeywordsByInsightId(id);
            return ResponseEntity.ok(keywords);
        } catch (Exception e) {
            log.error("Error getting insight keywords: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get insights by date range
     */
    @GetMapping("/date-range")
    @Operation(summary = "Get insights by date range", description = "Get insights within a specific date range")
    public ResponseEntity<List<Insight>> getInsightsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Insight> insights = insightsService.getInsightsByDateRange(startDate, endDate);
        return ResponseEntity.ok(insights);
    }
}
