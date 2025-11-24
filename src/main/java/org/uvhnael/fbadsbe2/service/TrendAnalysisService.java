package org.uvhnael.fbadsbe2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uvhnael.fbadsbe2.exception.CustomExceptions.NotFoundException;
import org.uvhnael.fbadsbe2.model.dto.TrendAnalysisResponse;
import org.uvhnael.fbadsbe2.model.entity.Ad;
import org.uvhnael.fbadsbe2.model.entity.TrendAnalysis;
import org.uvhnael.fbadsbe2.repository.AdsRepository;
import org.uvhnael.fbadsbe2.repository.TrendAnalysisRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendAnalysisService {
    
    private final AdsRepository adsRepository;
    private final TrendAnalysisRepository trendAnalysisRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Analyze weekly trends based on ads from the last 7 days
     */
    @Transactional
    public TrendAnalysis analyzeWeeklyTrends() {
        log.info("Starting weekly trend analysis");

        // Calculate date range (last 7 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        // Fetch ads from the last 7 days
        List<Ad> recentAds = adsRepository.findByTimeCreatedBetween(startDate, endDate);
        log.info("Found {} ads in the last 7 days", recentAds.size());

        // Limit to 50 ads to save tokens
        List<Ad> adsToAnalyze = recentAds.stream()
                .limit(50)
                .collect(Collectors.toList());

        // Concatenate typeAds and caption into a single prompt string
        StringBuilder adsContent = new StringBuilder();
        for (int i = 0; i < adsToAnalyze.size(); i++) {
            Ad ad = adsToAnalyze.get(i);
            adsContent.append(String.format("Ad %d:\n", i + 1));
            adsContent.append(String.format("Type: %s\n", ad.getTypeAds() != null ? ad.getTypeAds() : "N/A"));
            adsContent.append(String.format("Caption: %s\n\n", ad.getCaption() != null ? ad.getCaption() : "N/A"));
        }

        // Construct prompt asking for pure JSON response
        String prompt = buildAnalysisPrompt(adsContent.toString());

        // Call Gemini API to generate analysis
        String aiResponse = geminiService.generateText(prompt);

        if (aiResponse == null || aiResponse.isEmpty()) {
            log.error("Failed to get response from Gemini API");
            throw new RuntimeException("AI service returned empty response");
        }

        log.info("Received AI response, parsing JSON...");

        // Clean the response (remove ```json markdown blocks)
        String cleanedResponse = cleanJsonResponse(aiResponse);

        // Parse JSON into DTO
        TrendAnalysisResponse analysisResponse;
        try {
            analysisResponse = objectMapper.readValue(cleanedResponse, TrendAnalysisResponse.class);
            log.info("Successfully parsed trend analysis response");
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            log.debug("Cleaned response was: {}", cleanedResponse);
            throw new RuntimeException("Failed to parse AI response", e);
        }

        log.info("Analysis response: {}", analysisResponse);

        // Convert DTO fields to JSON strings and create entity
        TrendAnalysis trendAnalysis = convertToEntity(analysisResponse);

        log.info("Trend analysis: {}", trendAnalysis);
        
        // Save to database
        TrendAnalysis savedAnalysis = trendAnalysisRepository.save(trendAnalysis);
        log.info("Trend analysis saved with ID: {}", savedAnalysis.getId());

        return savedAnalysis;
    }

    /**
     * Build the prompt for AI analysis
     */
    private String buildAnalysisPrompt(String adsContent) {
        return String.format("""
                Phân tích dữ liệu quảng cáo Facebook từ 7 ngày qua và cung cấp insights bằng **định dạng JSON thuần túy**.
                
                Dữ liệu quảng cáo:
                %s
                
                Vui lòng phân tích dữ liệu và trả về CHỈ một JSON object (không có markdown, không có giải thích) với cấu trúc sau:
                {
                  "keywords": ["từ khóa 1", "từ khóa 2", "từ khóa 3"],
                  "topics": [
                    {
                      "name": "tên chủ đề",
                      "sentiment": "positive|negative|neutral",
                      "volume": 10
                    }
                  ],
                  "competitorSummary": "Tóm tắt hoạt động đối thủ và xu hướng thị trường",
                  "suggestions": "Gợi ý nội dung và khuyến nghị để cải thiện hiệu suất quảng cáo",
                  "aiSummary": "Tóm tắt tổng quan và insights chính từ phân tích",
                  "confidenceScore": 0.85
                }
                
                Hướng dẫn:
                - keywords: Trích xuất 5-10 từ khóa xu hướng nhất từ các quảng cáo (bằng tiếng Việt)
                - topics: Xác định 3-5 chủ đề chính với sentiment (positive/negative/neutral) và volume (số lần xuất hiện)
                - competitorSummary: Phân tích các mẫu, chiến lược đối thủ và định vị thị trường (bằng tiếng Việt)
                - suggestions: Cung cấp các khuyến nghị nội dung có thể thực hiện (bằng tiếng Việt)
                - aiSummary: Cung cấp tóm tắt toàn diện về xu hướng và insights (bằng tiếng Việt)
                - confidenceScore: Đánh giá độ tin cậy của phân tích này từ 0.0 đến 1.0
                
                Chỉ trả về JSON object, không có text hoặc formatting bổ sung. Tất cả nội dung văn bản phải bằng tiếng Việt.
                """, adsContent);
    }

    /**
     * Clean JSON response by removing markdown code blocks
     */
    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        
        // Remove ```json or ``` code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }

    /**
     * Convert TrendAnalysisResponse DTO to TrendAnalysis entity
     */
    private TrendAnalysis convertToEntity(TrendAnalysisResponse response) {
        try {
            // Convert competitorSummary and suggestions to JSON format (as JSON string value)
            String competitorActivityJson = objectMapper.writeValueAsString(response.getCompetitorSummary());
            String contentSuggestionsJson = objectMapper.writeValueAsString(response.getSuggestions());
            
            return TrendAnalysis.builder()
                    .analysisDate(LocalDate.now())
                    .trendingKeywords(objectMapper.writeValueAsString(response.getKeywords()))
                    .trendingTopics(objectMapper.writeValueAsString(response.getTopics()))
                    .competitorActivity(competitorActivityJson)
                    .contentSuggestions(contentSuggestionsJson)
                    .aiSummary(response.getAiSummary())
                    .confidenceScore(response.getConfidenceScore())
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Failed to convert DTO to entity: {}", e.getMessage());
            throw new RuntimeException("Failed to convert trend analysis to entity", e);
        }
    }

    /**
     * Get the most recent trend analysis
     */
    public TrendAnalysis getLatestTrend() {
        return trendAnalysisRepository.findTopByOrderByCreatedAtDesc()
            .orElseThrow(() -> new NotFoundException("No trend analysis found"));
    }

    /**
     * Get all trend analyses
     */
    public List<TrendAnalysis> getAllTrends() {
        return trendAnalysisRepository.findAll();
    }

    /**
     * Get trend by ID
     */
    public TrendAnalysis getTrendById(Long id) {
        return trendAnalysisRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Trend analysis not found with ID: " + id));
    }

    /**
     * Get trend analysis for a specific date
     */
    public Optional<TrendAnalysis> getTrendsByDate(LocalDate date) {
        return trendAnalysisRepository.findByAnalysisDate(date);
    }

    /**
     * Get the most recent trend analysis (backward compatibility)
     */
    public TrendAnalysis getCurrentTrends() {
        try {
            return getLatestTrend();
        } catch (NotFoundException e) {
            // Return a default/mock trend analysis if none exists
            log.warn("No trend analysis found, returning default");
            return createDefaultTrends();
        }
    }

    /**
     * Create default trend analysis (placeholder)
     */
    private TrendAnalysis createDefaultTrends() {
        return TrendAnalysis.builder()
            .analysisDate(LocalDate.now())
            .trendingKeywords("[\"giảm giá\", \"spa\", \"làm đẹp\", \"chăm sóc da\", \"ưu đãi\"]")
            .trendingTopics("[\"Ưu đãi cuối tuần\", \"Chăm sóc da mùa đông\"]")
            .contentSuggestions("[\"Bài viết về xu hướng skincare mùa đông\", \"Video hướng dẫn massage thư giãn\"]")
            .aiSummary("Current trends show interest in spa services and beauty care")
            .build();
    }
}
