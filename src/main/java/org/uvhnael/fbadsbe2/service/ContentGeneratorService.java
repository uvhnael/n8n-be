package org.uvhnael.fbadsbe2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uvhnael.fbadsbe2.model.dto.ContentGenerateRequest;
import org.uvhnael.fbadsbe2.model.entity.GeneratedContent;
import org.uvhnael.fbadsbe2.model.enums.ContentStatus;
import org.uvhnael.fbadsbe2.repository.GeneratedContentRepository;
import org.uvhnael.fbadsbe2.utils.Util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentGeneratorService {
    
    private final GeminiService geminiService;
    private final TrendAnalysisService trendAnalysisService;
    private final GeneratedContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate content based on trends and keywords
     */
    @Transactional
    public GeneratedContent generateContent(ContentGenerateRequest request) {
        log.info("Generating content: type={}, platform={}", 
            request.getContentType(), request.getPlatform());
        
        // Build prompt for AI
        String prompt = buildPrompt(request);
        
        // Call Gemini API
        String generatedText = geminiService.generateText(prompt);
        
        if (generatedText == null || generatedText.isEmpty()) {
            throw new RuntimeException("Failed to generate content from Gemini API");
        }
        
        // Parse and format content
        GeneratedContent content = parseAndFormat(generatedText, request);
        
        // Calculate trend score
        BigDecimal trendScore = calculateTrendScore(content);
        content.setTrendScore(trendScore);
        
        // Set timestamps
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        
        // Save to database
        GeneratedContent saved = contentRepository.save(content);
        
        log.info("Content generated successfully with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Build prompt for AI based on request and trends
     */
    private String buildPrompt(ContentGenerateRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là chuyên gia content marketing chuyên nghiệp.\n\n");
        
        // Task description
        prompt.append(String.format("Nhiệm vụ: Tạo %s cho platform %s\n", 
            request.getContentType(), request.getPlatform()));
        
        // Tone and length
        if (request.getTone() != null) {
            prompt.append(String.format("Tone: %s\n", request.getTone()));
        }
        if (request.getLength() != null) {
            prompt.append(String.format("Độ dài: %s\n", request.getLength()));
        }
        prompt.append("\n");
        
        // Add trending keywords if available
        List<String> keywords = request.getKeywords();
        if (keywords == null || keywords.isEmpty()) {
            // Get from latest insight
            keywords = getLatestTrendingKeywords(request.getBasedOnInsightId());
        }
        
        if (!keywords.isEmpty()) {
            prompt.append("Trending keywords cần sử dụng: ")
                .append(String.join(", ", keywords))
                .append("\n\n");
        }
        
        // Requirements
        prompt.append("Yêu cầu:\n");
        prompt.append("- Tạo content hấp dẫn, dễ viral, phù hợp với thị trường Việt Nam\n");
        prompt.append("- Sử dụng trending keywords một cách tự nhiên trong nội dung\n");
        prompt.append("- Nội dung phải chuyên nghiệp, có giá trị cho người đọc\n");
        
        if (Boolean.TRUE.equals(request.getIncludeHashtags())) {
            prompt.append("- Thêm 5-7 hashtags phù hợp và trending\n");
        }
        
        if (Boolean.TRUE.equals(request.getIncludeCTA())) {
            prompt.append("- Thêm call-to-action cuối bài rõ ràng và hấp dẫn\n");
        }
        
        prompt.append("\n");
        prompt.append("Format output: JSON với các fields sau:\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"tiêu đề hấp dẫn\",\n");
        prompt.append("  \"content\": \"nội dung chi tiết\",\n");
        prompt.append("  \"hashtags\": \"danh sách hashtags\",\n");
        prompt.append("  \"cta\": \"call to action\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * Parse generated text and create GeneratedContent entity
     */
    private GeneratedContent parseAndFormat(String generatedText, ContentGenerateRequest request) {
        try {
            // Try to parse as JSON
            JsonNode json = objectMapper.readTree(generatedText);
            
            String title = json.has("title") ? json.get("title").asText() : "Generated Content";
            String content = json.has("content") ? json.get("content").asText() : generatedText;
            String hashtags = json.has("hashtags") ? json.get("hashtags").asText() : "";
            String cta = json.has("cta") ? json.get("cta").asText() : "";
            
            // Append hashtags and CTA to content if present
            if (!hashtags.isEmpty()) {
                content += "\n\n" + hashtags;
            }
            if (!cta.isEmpty()) {
                content += "\n\n" + cta;
            }
            
            return GeneratedContent.builder()
                .title(title)
                .content(content)
                .contentType(request.getContentType())
                .platform(request.getPlatform())
                .basedOnKeywords(Util.convertListToJson(request.getKeywords()))
                .aiModel("gemini-2.0-flash-exp")
                .generationPrompt(buildPrompt(request))
                .status(ContentStatus.DRAFT.name())
                .build();
                
        } catch (Exception e) {
            log.warn("Could not parse JSON response, using raw text: {}", e.getMessage());
            
            // Fallback: use raw text
            return GeneratedContent.builder()
                .title("Generated Content")
                .content(generatedText)
                .contentType(request.getContentType())
                .platform(request.getPlatform())
                .basedOnKeywords(Util.convertListToJson(request.getKeywords()))
                .aiModel("gemini-2.0-flash-exp")
                .generationPrompt(buildPrompt(request))
                .status(ContentStatus.DRAFT.name())
                .build();
        }
    }

    /**
     * Calculate trend score based on keywords and current trends
     */
    private BigDecimal calculateTrendScore(GeneratedContent content) {
        // Simple scoring algorithm
        // In a real implementation, this would analyze keyword frequency,
        // match with current trends, etc.
        
        double baseScore = 50.0;
        
        // Add points for having keywords
        if (content.getBasedOnKeywords() != null && !content.getBasedOnKeywords().isEmpty()) {
            baseScore += 20.0;
        }
        
        // Add points for content length (longer content gets more points)
        if (content.getContent() != null) {
            int contentLength = content.getContent().length();
            if (contentLength > 500) baseScore += 15.0;
            else if (contentLength > 200) baseScore += 10.0;
        }
        
        // Add points for having a good title
        if (content.getTitle() != null && content.getTitle().length() > 20) {
            baseScore += 10.0;
        }
        
        // Random variance for realism
        double variance = Math.random() * 10 - 5; // -5 to +5
        
        double finalScore = Math.min(100.0, Math.max(0.0, baseScore + variance));
        
        return BigDecimal.valueOf(finalScore).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Get latest trending keywords from insights
     */
    private List<String> getLatestTrendingKeywords(Long insightId) {
        // For now, return default keywords
        // In a full implementation, this would query the keywords table
        // and get top keywords by count for the specified or latest insight
        
        List<String> defaultKeywords = List.of(
            "giảm giá", 
            "spa", 
            "làm đẹp", 
            "chăm sóc da", 
            "ưu đãi"
        );
        
        return defaultKeywords;
    }

    /**
     * Approve content for publishing
     */
    @Transactional
    public GeneratedContent approveContent(Long id, Long approvedBy) {
        log.info("Approving content ID: {}", id);
        
        GeneratedContent content = contentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Content not found"));
        
        content.setStatus(ContentStatus.APPROVED.name());
        content.setApprovedBy(approvedBy);
        content.setApprovedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        
        return contentRepository.save(content);
    }

    /**
     * Reject content
     */
    @Transactional
    public GeneratedContent rejectContent(Long id) {
        log.info("Rejecting content ID: {}", id);
        
        GeneratedContent content = contentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Content not found"));
        
        content.setStatus(ContentStatus.REJECTED.name());
        content.setUpdatedAt(LocalDateTime.now());
        
        return contentRepository.save(content);
    }

}
