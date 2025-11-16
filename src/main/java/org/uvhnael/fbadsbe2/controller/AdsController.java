package org.uvhnael.fbadsbe2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uvhnael.fbadsbe2.model.dto.AdDTO;
import org.uvhnael.fbadsbe2.model.entity.Ad;
import org.uvhnael.fbadsbe2.service.AdsService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ads Management", description = "APIs for managing Facebook Ads from n8n workflow")
public class AdsController {

    private final AdsService adsService;

    /**
     * Create new ad from n8n workflow
     */
    @PostMapping
    @Operation(summary = "Create ad from n8n", description = "Receives ad data from n8n workflow and saves to database")
    public ResponseEntity<?> createAd(@RequestBody AdDTO adDTO) {
        try {
            Ad ad = adsService.createAd(adDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(ad);
        } catch (Exception e) {
            log.error("Error creating ad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all ads with optional filters
     */
    @GetMapping
    @Operation(summary = "List all ads", description = "Get all ads with optional filters by type, status, or page name")
    public ResponseEntity<List<Ad>> getAllAds(
            @RequestParam(required = false) String typeAds,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pageName) {

        List<Ad> ads = adsService.getAllAds(typeAds, status, pageName);
        return ResponseEntity.ok(ads);
    }

    /**
     * Get ad by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get ad by ID", description = "Retrieve a specific ad by its database ID")
    public ResponseEntity<?> getAdById(@PathVariable Long id) {
        try {
            Ad ad = adsService.getAdById(id);
            return ResponseEntity.ok(ad);
        } catch (Exception e) {
            log.error("Error getting ad: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get ad by archive ID
     */
    @GetMapping("/archive/{adArchiveId}")
    @Operation(summary = "Get ad by archive ID", description = "Retrieve ad by Facebook archive ID")
    public ResponseEntity<?> getAdByArchiveId(@PathVariable String adArchiveId) {
        try {
            Ad ad = adsService.getAdByArchiveId(adArchiveId);
            return ResponseEntity.ok(ad);
        } catch (Exception e) {
            log.error("Error getting ad by archive ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update ad by ID
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update ad", description = "Update an existing ad by ID")
    public ResponseEntity<?> updateAd(
            @PathVariable Long id,
            @RequestBody AdDTO adDTO) {
        try {
            Ad updatedAd = adsService.updateAd(id, adDTO);
            return ResponseEntity.ok(updatedAd);
        } catch (Exception e) {
            log.error("Error updating ad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete ad by ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ad", description = "Delete an ad by ID")
    public ResponseEntity<?> deleteAd(@PathVariable Long id) {
        try {
            adsService.deleteAd(id);
            return ResponseEntity.ok(Map.of("message", "Ad deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting ad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get recent ads
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recent ads", description = "Get the 10 most recently scraped ads")
    public ResponseEntity<List<Ad>> getRecentAds() {
        List<Ad> ads = adsService.getRecentAds();
        return ResponseEntity.ok(ads);
    }

    /**
     * Search ads by keyword
     */
    @GetMapping("/search")
    @Operation(summary = "Search ads", description = "Search ads by keyword in caption")
    public ResponseEntity<List<Ad>> searchAds(@RequestParam String keyword) {
        List<Ad> ads = adsService.searchAds(keyword);
        return ResponseEntity.ok(ads);
    }

    /**
     * Get ads by date range
     */
    @GetMapping("/date-range")
    @Operation(summary = "Get ads by date range", description = "Get ads created within a specific date range")
    public ResponseEntity<List<Ad>> getAdsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Ad> ads = adsService.getAdsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ads);
    }

    /**
     * Get ad statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get ad statistics", description = "Get overall statistics about ads (total, by type, by status)")
    public ResponseEntity<Map<String, Object>> getAdStatistics() {
        Map<String, Object> stats = adsService.getAdStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get statistics by date range
     */
    @GetMapping("/stats/date-range")
    @Operation(summary = "Get statistics by date range", description = "Get statistics for ads within a date range")
    public ResponseEntity<Map<String, Object>> getStatisticsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> stats = adsService.getStatisticsByDateRange(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Bulk create ads from n8n
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk create ads", description = "Create multiple ads at once from n8n workflow")
    public ResponseEntity<?> bulkCreateAds(@RequestBody List<AdDTO> adDTOs) {
        try {
            List<Ad> createdAds = adDTOs.stream()
                    .map(adsService::createAd)
                    .toList();

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Created " + createdAds.size() + " ads successfully",
                    "ads", createdAds
            ));
        } catch (Exception e) {
            log.error("Error bulk creating ads: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update ad status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ad status", description = "Update only the status of an ad")
    public ResponseEntity<?> updateAdStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            AdDTO adDTO = new AdDTO();
            adDTO.setStatus(status);
            Ad updatedAd = adsService.updateAd(id, adDTO);
            return ResponseEntity.ok(updatedAd);
        } catch (Exception e) {
            log.error("Error updating ad status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
