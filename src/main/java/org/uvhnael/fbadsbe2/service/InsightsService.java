package org.uvhnael.fbadsbe2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uvhnael.fbadsbe2.exception.CustomExceptions.NotFoundException;
import org.uvhnael.fbadsbe2.model.entity.Ad;
import org.uvhnael.fbadsbe2.model.entity.Insight;
import org.uvhnael.fbadsbe2.model.entity.Keyword;
import org.uvhnael.fbadsbe2.repository.AdsRepository;
import org.uvhnael.fbadsbe2.repository.InsightsRepository;
import org.uvhnael.fbadsbe2.repository.KeywordsRepository;
import org.uvhnael.fbadsbe2.utils.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightsService {
    
    private final InsightsRepository insightsRepository;
    private final AdsRepository adsRepository;
    private final KeywordsRepository keywordsRepository;
    private final GeminiService geminiService;

    /**
     * Generate insight từ ads data trong khoảng thời gian
     */
    @Transactional
    public Insight generateInsight(LocalDate startDate, LocalDate endDate) {
        log.info("Generating insight for period: {} to {}", startDate, endDate);
        
        // Get ads in date range
        List<Ad> ads = adsRepository.findByTimeCreatedBetween(startDate, endDate);
        
        if (ads.isEmpty()) {
            throw new IllegalStateException("No ads found in the specified date range");
        }
        
        // Calculate statistics
        int totalAds = ads.size();
        long imageCount = ads.stream().filter(ad -> "IMAGE".equalsIgnoreCase(ad.getTypeAds())).count();
        long videoCount = ads.stream().filter(ad -> "VIDEO".equalsIgnoreCase(ad.getTypeAds())).count();
        long carouselCount = ads.stream().filter(ad -> "CAROUSEL".equalsIgnoreCase(ad.getTypeAds())).count();
        
        // Determine dominant format
        String dominantFormat = determineDominantFormat(imageCount, videoCount, carouselCount);
        
        // Calculate CTA rate (ads with CTA keywords)
        BigDecimal ctaRate = calculateCTARate(ads);
        
        // Find most active day
        String mostActiveDay = findMostActiveDay(ads);
        
        // Get week number
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = startDate.get(weekFields.weekOfWeekBasedYear());
        
        // Generate AI strategy report
        String aiStrategyReport = generateAIStrategyReport(ads, dominantFormat, ctaRate, mostActiveDay);
        
        // Create insight
        Insight insight = Insight.builder()
            .reportDate(endDate)
            .weekNumber(weekNumber)
            .totalAds(totalAds)
            .imageCount((int) imageCount)
            .videoCount((int) videoCount)
            .carouselCount((int) carouselCount)
            .dominantFormat(dominantFormat)
            .ctaRate(ctaRate)
            .mostActiveDay(mostActiveDay)
            .aiStrategyReport(aiStrategyReport)
            .createdAt(LocalDateTime.now())
            .build();
        
        insight = insightsRepository.save(insight);
        
        // Extract and save keywords
        extractAndSaveKeywords(insight, ads);
        
        log.info("Insight generated successfully with ID: {}", insight.getId());
        return insight;
    }

    /**
     * Generate insight cho tuần hiện tại
     */
    @Transactional
    public Insight generateCurrentWeekInsight() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        
        return generateInsight(startOfWeek, endOfWeek);
    }

    /**
     * Get latest insight
     */
    public Insight getLatestInsight() {
        return insightsRepository.findTopByOrderByCreatedAtDesc()
            .orElseThrow(() -> new NotFoundException("No insights found"));
    }

    /**
     * Get all insights
     */
    public List<Insight> getAllInsights() {
        return insightsRepository.findAll();
    }

    /**
     * Get insight by ID
     */
    public Insight getInsightById(Long id) {
        return insightsRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Insight not found with ID: " + id));
    }

    /**
     * Get keywords for specific insight
     */
    public List<Keyword> getKeywordsByInsightId(Long insightId) {
        // Verify insight exists
        getInsightById(insightId);
        
        return keywordsRepository.findByInsightIdOrderByCountDesc(insightId);
    }

    /**
     * Get insights by date range
     */
    public List<Insight> getInsightsByDateRange(LocalDate startDate, LocalDate endDate) {
        return insightsRepository.findByReportDateBetween(startDate, endDate);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Determine dominant format
     */
    private String determineDominantFormat(long imageCount, long videoCount, long carouselCount) {
        if (imageCount >= videoCount && imageCount >= carouselCount) {
            return "IMAGE";
        } else if (videoCount >= imageCount && videoCount >= carouselCount) {
            return "VIDEO";
        } else {
            return "CAROUSEL";
        }
    }

    /**
     * Calculate CTA rate
     */
    private BigDecimal calculateCTARate(List<Ad> ads) {
        // 1. Kiểm tra đầu vào an toàn
        if (ads == null || ads.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Use CTA keywords from Util
        List<String> beautyCTAKeywords = Util.getBeautyCTAKeywords();

        // 3. Lọc và đếm số lượng Ads có chứa CTA
        long adsWithCTA = ads.stream()
                .filter(Objects::nonNull) // Bỏ qua các object Ad bị null
                .map(Ad::getCaption)      // Lấy nội dung caption
                .filter(caption -> Util.containsAnyKeyword(caption, beautyCTAKeywords)) // Kiểm tra CTA
                .count();

        // 4. Tính phần trăm: (Số Ads có CTA / Tổng số Ads) * 100
        return BigDecimal.valueOf(adsWithCTA)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(ads.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Find most active day
     */
    private String findMostActiveDay(List<Ad> ads) {
        Map<DayOfWeek, Long> dayCount = ads.stream()
            .filter(ad -> ad.getTimeCreated() != null)
            .collect(Collectors.groupingBy(
                ad -> ad.getTimeCreated().getDayOfWeek(),
                Collectors.counting()
            ));
        
        if (dayCount.isEmpty()) {
            return "UNKNOWN";
        }
        
        DayOfWeek mostActiveDay = dayCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(DayOfWeek.MONDAY);
        
        // Convert to Vietnamese
        return Util.dayOfWeekToVietnamese(mostActiveDay);
    }

    /**
     * Generate AI strategy report using Gemini
     */
    private String generateAIStrategyReport(List<Ad> ads, String dominantFormat, 
                                           BigDecimal ctaRate, String mostActiveDay) {
        try {
            String prompt = String.format("""
                Bạn là chuyên gia phân tích Facebook Ads.
                
                Dữ liệu phân tích:
                - Tổng số ads: %d
                - Format phổ biến nhất: %s
                - Tỷ lệ ads có CTA: %.2f%%
                - Ngày đăng ads nhiều nhất: %s
                
                Hãy đưa ra chiến lược marketing chi tiết:
                1. Phân tích xu hướng hiện tại
                2. Đề xuất format nội dung tối ưu
                3. Gợi ý thời điểm đăng bài tốt nhất
                4. Chiến lược CTA hiệu quả
                5. Các điểm cần cải thiện
                
                Trả lời ngắn gọn, súc tích, dưới 500 từ.
                """,
                ads.size(), dominantFormat, ctaRate, mostActiveDay
            );
            
            String aiResponse = geminiService.generateText(prompt);
            return aiResponse != null ? aiResponse : generateDefaultStrategyReport(dominantFormat, ctaRate, mostActiveDay);
            
        } catch (Exception e) {
            log.error("Error generating AI strategy report: {}", e.getMessage());
            return generateDefaultStrategyReport(dominantFormat, ctaRate, mostActiveDay);
        }
    }

    /**
     * Generate default strategy report (fallback)
     */
    private String generateDefaultStrategyReport(String dominantFormat, 
                                                BigDecimal ctaRate, String mostActiveDay) {
        return String.format("""
            📊 PHÂN TÍCH CHIẾN LƯỢC:
            
            1. XU HƯỚNG HIỆN TẠI:
            - Format %s đang được sử dụng phổ biến nhất
            - Tỷ lệ ads có CTA: %.2f%%
            - Ngày hoạt động mạnh nhất: %s
            
            2. ĐỀ XUẤT:
            - Tiếp tục tận dụng format %s cho hiệu quả cao
            - %s
            - Tập trung đăng bài vào %s để tăng engagement
            
            3. CẢI THIỆN:
            - Đa dạng hóa format nội dung
            - Tối ưu CTA để tăng conversion
            - A/B testing các thời điểm đăng bài khác nhau
            """,
            dominantFormat, ctaRate, mostActiveDay,
            dominantFormat,
            ctaRate.compareTo(BigDecimal.valueOf(50)) < 0 
                ? "Cần tăng cường CTA trong nội dung" 
                : "Duy trì chiến lược CTA hiện tại",
            mostActiveDay
        );
    }

    /**
     * Extract keywords from ads captions and save
     */
    private void extractAndSaveKeywords(Insight insight, List<Ad> ads) {
        Map<String, Integer> keywordCount = new HashMap<>();
        
        // Get stop words from Util
        Set<String> stopWords = Util.getVietnameseStopWords();
        
        // Extract keywords from captions
        for (Ad ad : ads) {
            if (ad.getCaption() != null && !ad.getCaption().isEmpty()) {
                String[] words = Util.extractKeywords(ad.getCaption());
                
                for (String word : words) {
                    keywordCount.merge(word, 1, Integer::sum);
                }
            }
        }
        
        // Get top 20 keywords
        List<Map.Entry<String, Integer>> topKeywords = keywordCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(20)
            .toList();
        
        // Save keywords
        int totalWords = keywordCount.values().stream().mapToInt(Integer::intValue).sum();
        
        for (Map.Entry<String, Integer> entry : topKeywords) {
            BigDecimal percentage = totalWords > 0 
                ? BigDecimal.valueOf(entry.getValue())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalWords), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            
            Keyword keyword = Keyword.builder()
                .insightId(insight.getId())
                .keyword(entry.getKey())
                .count(entry.getValue())
                .percentage(percentage)
                .week("W" + insight.getWeekNumber())
                .build();
            
            keywordsRepository.save(keyword);
        }
        
        log.info("Saved {} keywords for insight ID: {}", topKeywords.size(), insight.getId());
    }
}
