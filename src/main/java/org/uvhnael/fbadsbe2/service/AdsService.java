package org.uvhnael.fbadsbe2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uvhnael.fbadsbe2.exception.CustomExceptions.NotFoundException;
import org.uvhnael.fbadsbe2.exception.CustomExceptions.ValidationException;
import org.uvhnael.fbadsbe2.model.dto.AdDTO;
import org.uvhnael.fbadsbe2.model.entity.Ad;
import org.uvhnael.fbadsbe2.repository.AdsRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdsService {

    private final AdsRepository adsRepository;

    /**
     * Create new ad from n8n workflow
     */
    @Transactional
    public Ad createAd(AdDTO adDTO) {
        log.info("Creating ad with archive ID: {}", adDTO.getAdArchiveId());

        // Validate required fields
        if (adDTO.getAdArchiveId() == null || adDTO.getAdArchiveId().isEmpty()) {
            throw new ValidationException("Ad archive ID is required");
        }

        // Check if ad already exists
        if (adsRepository.findByAdArchiveId(adDTO.getAdArchiveId()).isPresent()) {
            log.warn("Ad with archive ID {} already exists, updating instead", adDTO.getAdArchiveId());
            return updateExistingAd(adDTO);
        }

        // Create new ad
        Ad ad = Ad.builder()
                .adArchiveId(adDTO.getAdArchiveId())
                .pageName(adDTO.getPageName())
                .caption(adDTO.getCaption())
                .typeAds(adDTO.getTypeAds() != null ? adDTO.getTypeAds() : "UNKNOWN")
                .urlAdsPost(adDTO.getUrlAdsPost())
                .aiAnalyze(adDTO.getAiAnalyze())
                .imgUrl(adDTO.getImgUrl())
                .videoUrl(adDTO.getVideoUrl())
                .status(adDTO.getStatus() != null ? adDTO.getStatus() : "ACTIVE")
                .timeCreated(adDTO.getTimeCreated() != null ? adDTO.getTimeCreated() : LocalDate.now())
                .scrapedAt(LocalDateTime.now())
                .build();

        ad = adsRepository.save(ad);
        log.info("Ad created successfully with ID: {}", ad.getId());

        return ad;
    }

    /**
     * Update existing ad
     */
    @Transactional
    public Ad updateExistingAd(AdDTO adDTO) {
        Ad existingAd = adsRepository.findByAdArchiveId(adDTO.getAdArchiveId())
                .orElseThrow(() -> new NotFoundException("Ad not found"));

        // Update fields
        if (adDTO.getPageName() != null) existingAd.setPageName(adDTO.getPageName());
        if (adDTO.getCaption() != null) existingAd.setCaption(adDTO.getCaption());
        if (adDTO.getTypeAds() != null) existingAd.setTypeAds(adDTO.getTypeAds());
        if (adDTO.getUrlAdsPost() != null) existingAd.setUrlAdsPost(adDTO.getUrlAdsPost());
        if (adDTO.getAiAnalyze() != null) existingAd.setAiAnalyze(adDTO.getAiAnalyze());
        if (adDTO.getImgUrl() != null) existingAd.setImgUrl(adDTO.getImgUrl());
        if (adDTO.getVideoUrl() != null) existingAd.setVideoUrl(adDTO.getVideoUrl());
        if (adDTO.getStatus() != null) existingAd.setStatus(adDTO.getStatus());
        if (adDTO.getTimeCreated() != null) existingAd.setTimeCreated(adDTO.getTimeCreated());

        existingAd.setScrapedAt(LocalDateTime.now());

        return adsRepository.save(existingAd);
    }

    /**
     * Get all ads with optional filters
     */
    public List<Ad> getAllAds(String typeAds, String status, String pageName) {
        if (typeAds != null) {
            return adsRepository.findByTypeAds(typeAds);
        } else if (status != null) {
            return adsRepository.findByStatus(status);
        } else if (pageName != null) {
            return adsRepository.findByPageName(pageName);
        }
        return adsRepository.findAll();
    }

    /**
     * Get ad by ID
     */
    public Ad getAdById(Long id) {
        return adsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ad not found with ID: " + id));
    }

    /**
     * Get ad by archive ID
     */
    public Ad getAdByArchiveId(String adArchiveId) {
        return adsRepository.findByAdArchiveId(adArchiveId)
                .orElseThrow(() -> new NotFoundException("Ad not found with archive ID: " + adArchiveId));
    }

    /**
     * Update ad by ID
     */
    @Transactional
    public Ad updateAd(Long id, AdDTO adDTO) {
        log.info("Updating ad with ID: {}", id);

        Ad ad = getAdById(id);

        // Update fields if provided
        if (adDTO.getPageName() != null) ad.setPageName(adDTO.getPageName());
        if (adDTO.getCaption() != null) ad.setCaption(adDTO.getCaption());
        if (adDTO.getTypeAds() != null) ad.setTypeAds(adDTO.getTypeAds());
        if (adDTO.getUrlAdsPost() != null) ad.setUrlAdsPost(adDTO.getUrlAdsPost());
        if (adDTO.getAiAnalyze() != null) ad.setAiAnalyze(adDTO.getAiAnalyze());
        if (adDTO.getImgUrl() != null) ad.setImgUrl(adDTO.getImgUrl());
        if (adDTO.getVideoUrl() != null) ad.setVideoUrl(adDTO.getVideoUrl());
        if (adDTO.getStatus() != null) ad.setStatus(adDTO.getStatus());

        ad = adsRepository.save(ad);
        log.info("Ad updated successfully");

        return ad;
    }

    /**
     * Delete ad by ID
     */
    @Transactional
    public void deleteAd(Long id) {
        log.info("Deleting ad with ID: {}", id);

        if (!adsRepository.existsById(id)) {
            throw new NotFoundException("Ad not found with ID: " + id);
        }

        adsRepository.deleteById(id);
        log.info("Ad deleted successfully");
    }

    /**
     * Get recent ads (last 10)
     */
    public List<Ad> getRecentAds() {
        return adsRepository.findTop10ByOrderByScrapedAtDesc();
    }

    /**
     * Search ads by keyword in caption
     */
    public List<Ad> searchAds(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return adsRepository.findAll();
        }
        return adsRepository.findByCaptionContainingIgnoreCase(keyword);
    }

    /**
     * Get ads by date range
     */
    public List<Ad> getAdsByDateRange(LocalDate startDate, LocalDate endDate) {
        return adsRepository.findByTimeCreatedBetween(startDate, endDate);
    }

    /**
     * Get ad statistics
     */
    public Map<String, Object> getAdStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Total ads
        long totalAds = adsRepository.count();
        stats.put("totalAds", totalAds);

        // Count by type
        List<Object[]> typeStats = adsRepository.countByType();
        Map<String, Long> typeCount = new HashMap<>();
        for (Object[] row : typeStats) {
            typeCount.put((String) row[0], (Long) row[1]);
        }
        stats.put("byType", typeCount);

        // Count by status
        long activeAds = adsRepository.countByStatus("ACTIVE");
        long inactiveAds = adsRepository.countByStatus("INACTIVE");
        stats.put("activeAds", activeAds);
        stats.put("inactiveAds", inactiveAds);

        // Recent ads count (last 7 days)
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Ad> recentAds = adsRepository.findByTimeCreatedBetween(sevenDaysAgo, LocalDate.now());
        stats.put("recentAds", recentAds.size());

        return stats;
    }

    /**
     * Get statistics by date range
     */
    public Map<String, Object> getStatisticsByDateRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        List<Object[]> typeStats = adsRepository.getAdStatisticsByDateRange(startDate, endDate);

        long totalAds = 0;
        Map<String, Long> typeCount = new HashMap<>();

        for (Object[] row : typeStats) {
            Long count = (Long) row[0];
            String type = (String) row[1];

            totalAds += count;
            typeCount.put(type, count);
        }

        stats.put("totalAds", totalAds);
        stats.put("byType", typeCount);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);

        return stats;
    }

    /**
     * Convert Ad entity to DTO
     */
    public AdDTO convertToDTO(Ad ad) {
        return AdDTO.builder()
                .id(ad.getId())
                .adArchiveId(ad.getAdArchiveId())
                .pageName(ad.getPageName())
                .caption(ad.getCaption())
                .typeAds(ad.getTypeAds())
                .urlAdsPost(ad.getUrlAdsPost())
                .aiAnalyze(ad.getAiAnalyze())
                .imgUrl(ad.getImgUrl())
                .videoUrl(ad.getVideoUrl())
                .status(ad.getStatus())
                .timeCreated(ad.getTimeCreated())
                .scrapedAt(ad.getScrapedAt())
                .build();
    }

    /**
     * Convert list of Ad entities to DTOs
     */
    public List<AdDTO> convertToDTOList(List<Ad> ads) {
        return ads.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check existence of ads by archive IDs
     */
    public Map<String, Boolean> checkAdsExistence(List<String> adArchiveIds) {
        log.info("Checking existence for {} ad archive IDs", adArchiveIds.size());
        
        Map<String, Boolean> existenceMap = new HashMap<>();
        
        for (String adArchiveId : adArchiveIds) {
            boolean exists = adsRepository.findByAdArchiveId(adArchiveId).isPresent();
            existenceMap.put(adArchiveId, exists);
        }
        
        long existingCount = existenceMap.values().stream().filter(Boolean::booleanValue).count();
        log.info("Found {} existing ads out of {} checked", existingCount, adArchiveIds.size());
        
        return existenceMap;
    }
}
