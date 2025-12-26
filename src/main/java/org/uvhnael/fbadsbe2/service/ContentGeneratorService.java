package org.uvhnael.fbadsbe2.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uvhnael.fbadsbe2.exception.CustomExceptions;
import org.uvhnael.fbadsbe2.model.dto.ContentGenerateRequest;
import org.uvhnael.fbadsbe2.model.entity.GeneratedContent;
import org.uvhnael.fbadsbe2.model.entity.TrendAnalysis;
import org.uvhnael.fbadsbe2.model.enums.ContentStatus;
import org.uvhnael.fbadsbe2.repository.GeneratedContentRepository;
import org.uvhnael.fbadsbe2.repository.TrendAnalysisRepository;
import org.uvhnael.fbadsbe2.utils.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentGeneratorService {
    
    private final GeminiService geminiService;
    private final TrendAnalysisRepository trendAnalysisRepository;
    private final GeneratedContentRepository contentRepository;
    private final ObjectMapper objectMapper;



    @Value("${gemini.api.model:gemini-2.0-flash-exp}")
    private String model;

    /**
     * Generate content based on trends and keywords
     */
    @Transactional
    public GeneratedContent generateContent(ContentGenerateRequest request) {
        log.info("Generating content: type={}, platform={}", 
            request.getContentType(), request.getPlatform());
        
        // Step 1: Keyword Logic - Check if valid keywords provided, otherwise fetch from DB
        List<String> keywords = request.getKeywords();
        Long insightId = request.getBasedOnTrendAnalysisId();
        
        if (keywords == null || keywords.isEmpty()) {
            // Fetch keywords from specific insight or latest trend
            if (!Util.isNullOrZero(insightId)) {
                keywords = getKeywordsFromInsight(insightId);
                log.info("Fetched {} keywords from insight ID: {}", keywords.size(), insightId);
            } else {
                keywords = getLatestTrendingKeywords();
                log.info("Fetched {} trending keywords from latest analysis", keywords.size());
            }
            request.setKeywords(keywords);
        } else {
            log.info("Using {} user-provided keywords", keywords.size());
        }
        
        // Step 2: Build prompt for AI
        String prompt = buildPrompt(request, keywords);
        
        // Step 3: Call Gemini API
        String generatedText = geminiService.generateText(prompt);
        
        if (Util.isNullOrBlank(generatedText)) {
            throw new RuntimeException("Failed to generate content from Gemini API");
        }
        
        // Step 4: Clean JSON response
        String cleanedText = Util.cleanJsonResponse(generatedText);
        log.debug("Cleaned AI response: {}", cleanedText);
        
        // Step 5: Parse and format content
        String trendingTopicsJson = extractTrendingTopics(request.getBasedOnTrendAnalysisId());
        GeneratedContent content = parseAndFormat(cleanedText, request, prompt, trendingTopicsJson);
        
        // Step 6: Calculate trend score
        BigDecimal trendScore = calculateTrendScore(content, keywords);
        content.setTrendScore(trendScore);
        
        // Step 7: Save to database
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        GeneratedContent saved = contentRepository.save(content);
        
        log.info("Content generated successfully with ID: {}, TrendScore: {}", 
            saved.getId(), trendScore);
        return saved;
    }

    /**
     * Build prompt for AI based on request and keywords
     * Prompt Engineering: Construct detailed prompt for Gemini AI
     */
    private String buildPrompt(ContentGenerateRequest request, List<String> keywords) {
        StringBuilder prompt = new StringBuilder();
        
        // Role: Expert Copywriter with 10 years experience
        prompt.append("Bạn là Expert Copywriter với 10 năm kinh nghiệm chuyên viết content marketing ")
              .append("chuyển đổi cao, đặc biệt là Facebook Ads cho Thẩm mỹ VT trên thị trường Việt Nam.\n\n");
        
        // Task description
        prompt.append("=== NHIỆM VỤ ===\n");
        prompt.append(String.format("Tạo %s cho nền tảng %s\n", 
            request.getContentType(), request.getPlatform()));
        
        // Tone and style
        if (!Util.isNullOrBlank(request.getTone())) {
            prompt.append(String.format("Giọng điệu: %s\n", request.getTone()));
        }
        
        if (!Util.isNullOrBlank(request.getLength())) {
            prompt.append(String.format("Độ dài: %s\n", request.getLength()));
        }
        prompt.append("\n");

        // Add trending topics if based on specific insight
        if(!Util.isNullOrZero(request.getBasedOnTrendAnalysisId())) {
            try {
                TrendAnalysis trend = trendAnalysisRepository.findById(request.getBasedOnTrendAnalysisId())
                        .orElseThrow(() -> new CustomExceptions.NotFoundException(
                            "Trend analysis not found with ID: " + request.getBasedOnTrendAnalysisId()));

                if (!Util.isNullOrBlank(trend.getTrendingTopics())) {
                    prompt.append("=== XU HƯỚNG NỔI BẬT TỪ PHÂN TÍCH THỊ TRƯỜNG ===\n");
                    prompt.append("Dưới đây là các chủ đề xu hướng nổi bật được phân tích từ dữ liệu thị trường:\n");
                    
                    List<JsonNode> topics = objectMapper.readValue(
                            trend.getTrendingTopics(),
                            new TypeReference<List<JsonNode>>() {}
                    );
                    
                    for (int i = 0; i < topics.size(); i++) {
                        String topicName = topics.get(i).get("name").asText();
                        prompt.append(String.format("%d. %s\n", i + 1, topicName));
                    }
                    prompt.append("\n⚠️ Hãy tham khảo và kết hợp các xu hướng trên vào nội dung một cách sáng tạo.\n\n");
                }
            } catch (Exception e) {
                log.error("Error adding trending topics to prompt: {}", e.getMessage());
            }
        }
        
        // Keywords injection
        if (keywords != null && !keywords.isEmpty()) {
            prompt.append("=== TRENDING KEYWORDS (BẮT BUỘC SỬ DỤNG) ===\n");
            prompt.append("Các từ khóa xu hướng sau ĐÃ được phân tích từ dữ liệu thực tế:\n");
            for (int i = 0; i < keywords.size(); i++) {
                prompt.append(String.format("%d. %s\n", i + 1, keywords.get(i)));
            }
            prompt.append("\n⚠️ Hãy sử dụng ít nhất 3-5 từ khóa trên một cách TỰ NHIÊN trong nội dung.\n\n");
        }
        
        // Requirements
        prompt.append("=== YÊU CẦU ===\n");
        prompt.append("✓ Tạo nội dung hấp dẫn, dễ lan truyền (viral), phù hợp văn hóa Việt Nam\n");
        prompt.append("✓ Sử dụng trending keywords một cách tự nhiên, không gượng ép\n");
        prompt.append("✓ Nội dung phải chuyên nghiệp, mang lại giá trị thực cho người đọc\n");
        prompt.append("✓ Tối ưu cho thuật toán Facebook (engagement-driven)\n");
        
        if (Boolean.TRUE.equals(request.getIncludeHashtags())) {
            prompt.append("✓ Bao gồm 5-7 hashtags phù hợp và trending\n");
        }
        
        if (Boolean.TRUE.equals(request.getIncludeCTA())) {
            prompt.append("✓ Kết thúc bằng call-to-action rõ ràng, hấp dẫn, tạo động lực hành động ngay\n");
        }
        prompt.append("\n");
        
        // Critical output format instruction
        prompt.append("=== QUAN TRỌNG: FORMAT OUTPUT ===\n");
        prompt.append("⚠️ CHỈ TRẢ VỀ JSON THUẦN TÚY - KHÔNG thêm markdown, KHÔNG giải thích, KHÔNG ```json\n");
        prompt.append("Cấu trúc JSON bắt buộc:\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"Tiêu đề hấp dẫn (10-70 ký tự)\",\n");
        prompt.append("  \"content\": \"Nội dung chi tiết, sử dụng trending keywords\",\n");
        prompt.append("  \"hashtags\": \"#hashtag1 #hashtag2 #hashtag3...\",\n");
        prompt.append("  \"cta\": \"Call-to-action mạnh mẽ\",\n");
        prompt.append("  \"image_prompt\": \"Prompt để tạo ảnh liên quan đến nội dung này\"\n");
        prompt.append("}\n\n");
        prompt.append("Bắt đầu ngay bằng ký tự { và kết thúc bằng }. Không thêm bất kỳ text nào khác!\n");
        
        log.debug("Constructed prompt for AI: {}", prompt.toString());
        return prompt.toString();
    }

    /**
     * Parse generated text and create GeneratedContent entity
     */
    private GeneratedContent parseAndFormat(String generatedText, ContentGenerateRequest request, String prompt, String trendingTopicsJson) {
        try {
            // Try to parse as JSON
            JsonNode json = objectMapper.readTree(generatedText);
            
            String title = json.has("title") ? json.get("title").asText() : "Nội dung được tạo tự động";
            String content = json.has("content") ? json.get("content").asText() : generatedText;
            String hashtags = json.has("hashtags") ? json.get("hashtags").asText() : "";
            String cta = json.has("cta") ? json.get("cta").asText() : "";
            String imagePrompt = json.has("image_prompt") ? json.get("image_prompt").asText() : "";
            
            // Combine content with hashtags and CTA
            StringBuilder finalContent = new StringBuilder(content);
            
            if (!Util.isNullOrBlank(cta)) {
                finalContent.append("\n\n").append(cta);
            }
            
            if (!Util.isNullOrBlank(hashtags)) {
                finalContent.append("\n\n").append(hashtags);
            }
            
            log.info("Successfully parsed JSON response");
            
            return GeneratedContent.builder()
                .title(title)
                .content(finalContent.toString())
                .contentType(request.getContentType())
                .platform(request.getPlatform())
                .basedOnTrends(trendingTopicsJson)
                .basedOnKeywords(Util.convertListToJson(request.getKeywords()))
                .aiModel(model)
                .generationPrompt(prompt)
                .imagePrompt(imagePrompt)
                .status(ContentStatus.DRAFT.name())
                .build();
                
        } catch (Exception e) {
            log.warn("Could not parse JSON response, using fallback mechanism: {}", e.getMessage());
            
            // Fallback: use raw text as content
            return GeneratedContent.builder()
                .title("Nội dung được tạo tự động")
                .content(generatedText)
                .contentType(request.getContentType())
                .platform(request.getPlatform())
                .basedOnTrends(trendingTopicsJson)
                .basedOnKeywords(Util.convertListToJson(request.getKeywords()))
                .aiModel(model)
                .generationPrompt(prompt)
                .imagePrompt("")
                .status(ContentStatus.DRAFT.name())
                .build();
        }
    }

    /**
     * Calculate trend score based on keywords and content quality
     */
    private BigDecimal calculateTrendScore(GeneratedContent content, List<String> keywords) {
        double baseScore = 50.0;
        
        String fullContent = (content.getContent() + " " + content.getTitle()).toLowerCase();
        
        // Add points for each keyword found in content (10 points per keyword)
        if (keywords != null && !keywords.isEmpty()) {
            int keywordMatches = 0;
            for (String keyword : keywords) {
                if (fullContent.contains(keyword.toLowerCase())) {
                    keywordMatches++;
                }
            }
            double keywordScore = Math.min(40.0, keywordMatches * 10.0);
            baseScore += keywordScore;
            log.debug("Keyword matches: {}/{}, Score: +{}", keywordMatches, keywords.size(), keywordScore);
        }
        
        // Add bonus for optimal content length (>300 chars)
        if (!Util.isNullOrBlank(content.getContent())) {
            int contentLength = content.getContent().length();
            if (contentLength > 300) {
                baseScore += 10.0;
                log.debug("Content length bonus: +10 (length: {})", contentLength);
            }
        }
        
        // Add bonus for optimized title length (10-70 chars)
        if (!Util.isNullOrBlank(content.getTitle())) {
            int titleLength = content.getTitle().length();
            if (titleLength >= 10 && titleLength <= 70) {
                baseScore += 5.0;
                log.debug("Title length bonus: +5 (length: {})", titleLength);
            }
        }
        
        // Cap at 100
        double finalScore = Math.min(100.0, baseScore);
        
        return BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Extract trending topics from insight and return as JSON string
     */
    private String extractTrendingTopics(Long insightId) {
        if (Util.isNullOrZero(insightId)) {
            return null;
        }
        
        try {
            TrendAnalysis trend = trendAnalysisRepository.findById(insightId)
                .orElse(null);
            
            if (trend != null && !Util.isNullOrBlank(trend.getTrendingTopics())) {
                // Parse and extract only topic names
                List<JsonNode> topics = objectMapper.readValue(
                    trend.getTrendingTopics(),
                    new TypeReference<List<JsonNode>>() {}
                );
                
                List<String> topicNames = new ArrayList<>();
                for (JsonNode topic : topics) {
                    if (topic.has("name")) {
                        topicNames.add(topic.get("name").asText());
                    }
                }
                
                String result = Util.convertListToJson(topicNames);
                log.debug("Extracted {} trending topics from insight ID: {}", topicNames.size(), insightId);
                return result;
            }
            
        } catch (Exception e) {
            log.error("Error extracting trending topics from insight ID {}: {}", insightId, e.getMessage());
        }
        
        return null;
    }

    /**
     * Get keywords from specific insight ID
     */
    private List<String> getKeywordsFromInsight(Long insightId) {
        try {
            TrendAnalysis trend = trendAnalysisRepository.findById(insightId)
                .orElseThrow(() -> new CustomExceptions.NotFoundException(
                    "Trend analysis not found with ID: " + insightId));
            
            if (!Util.isNullOrBlank(trend.getTrendingKeywords())) {
                // Parse JSON string to List
                List<String> keywords = objectMapper.readValue(
                    trend.getTrendingKeywords(), 
                    new TypeReference<List<String>>() {}
                );
                
                log.info("Loaded {} keywords from insight ID: {}", keywords.size(), insightId);
                return keywords;
            }
            
        } catch (Exception e) {
            log.error("Error fetching keywords from insight ID {}: {}", insightId, e.getMessage());
        }
        
        // Fallback to default keywords
        log.warn("Using default fallback keywords for insight ID: {}", insightId);
        return getDefaultKeywords();
    }

    /**
     * Get latest trending keywords from database
     */
    private List<String> getLatestTrendingKeywords() {
        try {
            // Query the latest trend analysis
            TrendAnalysis latestTrend = trendAnalysisRepository.findTopByOrderByAnalysisDateDesc()
                .orElse(null);
            
            if (latestTrend != null && !Util.isNullOrBlank(latestTrend.getTrendingKeywords())) {
                // Parse JSON string to List
                List<String> keywords = objectMapper.readValue(
                    latestTrend.getTrendingKeywords(), 
                    new TypeReference<List<String>>() {}
                );
                
                log.info("Loaded {} keywords from latest trend analysis (ID: {})", 
                    keywords.size(), latestTrend.getId());
                return keywords;
            }
            
        } catch (Exception e) {
            log.error("Error fetching trending keywords from database: {}", e.getMessage());
        }
        
        // Fallback to default keywords
        log.warn("Using default fallback keywords");
        return getDefaultKeywords();
    }

    /**
     * Get default fallback keywords
     */
    private List<String> getDefaultKeywords() {
        return new ArrayList<>(List.of(
            "khuyến mãi",
            "giảm giá", 
            "ưu đãi",
            "làm đẹp", 
            "spa",
            "chăm sóc da",
            "voucher",
            "flash sale"
        ));
    }

    /**
     * Approve content for publishing
     */
    @Transactional
    public GeneratedContent approveContent(Long id, Long approvedBy) {
        log.info("Approving content ID: {} by user: {}", id, approvedBy);
        
        GeneratedContent content = contentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Content not found with ID: " + id));
        
        content.setStatus(ContentStatus.APPROVED.name());
        content.setApprovedBy(approvedBy);
        content.setApprovedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        
        GeneratedContent saved = contentRepository.save(content);
        log.info("Content ID: {} approved successfully", id);
        
        return saved;
    }

    /**
     * Reject content
     */
    @Transactional
    public GeneratedContent rejectContent(Long id) {
        log.info("Rejecting content ID: {}", id);
        
        GeneratedContent content = contentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Content not found with ID: " + id));
        
        content.setStatus(ContentStatus.REJECTED.name());
        content.setUpdatedAt(LocalDateTime.now());
        
        GeneratedContent saved = contentRepository.save(content);
        log.info("Content ID: {} rejected", id);
        
        return saved;
    }
}
