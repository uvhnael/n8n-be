package org.uvhnael.fbadsbe2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${gemini.api.key:}")
    private String apiKey;
    
    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;
    
    @Value("${gemini.api.model:gemini-2.0-flash-exp}")
    private String model;

    /**
     * Generate text content using Gemini API
     */
    public String generateText(String prompt) {
        log.info("Generating text with Gemini API");
        
        // Check if API key is configured
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured. Returning mock response.");
            return generateMockResponse(prompt);
        }
        
        try {
            // Build request URL
            String url = String.format("%s/v1beta/models/%s:generateContent?key=%s", 
                baseUrl, model, apiKey);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", prompt)));
            requestBody.put("contents", List.of(content));
            
            // Set generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 2048);
            requestBody.put("generationConfig", generationConfig);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            } else {
                log.error("Unexpected response from Gemini API: {}", response.getStatusCode());
                return generateMockResponse(prompt);
            }
            
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return generateMockResponse(prompt);
        }
    }

    /**
     * Extract generated text from Gemini API response
     */
    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
            
            log.error("Could not extract text from Gemini response");
            return null;
            
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate mock response when API is not available
     */
    private String generateMockResponse(String prompt) {
        log.info("Generating mock response (API key not configured)");
        
        String mockTitle = "🌟 Ưu đãi đặc biệt cuối tuần - Giảm giá lên đến 50%!";
        String mockContent = """
            Chào mừng khách hàng đến với chương trình ưu đãi đặc biệt! 🎉
            
            ✨ Giảm giá lên đến 50% cho tất cả dịch vụ spa cao cấp
            ⏰ Thời gian có hạn: Chỉ trong cuối tuần này
            💝 Tặng kèm voucher trị giá 200k cho lần sử dụng tiếp theo
            
            Đừng bỏ lỡ cơ hội vàng này để chăm sóc bản thân! 
            Đặt lịch ngay hôm nay để nhận ưu đãi tốt nhất.
            
            📞 Liên hệ: 0123-456-789
            📍 Địa chỉ: 123 Đường ABC, Quận XYZ
            
            #spa #giamgia #beauty #chamsocbanhan #cuoituan
            """;
        
        try {
            Map<String, String> mockResponse = new HashMap<>();
            mockResponse.put("title", mockTitle);
            mockResponse.put("content", mockContent);
            mockResponse.put("hashtags", "#spa #giamgia #beauty #chamsocbanhan #cuoituan");
            mockResponse.put("cta", "Đặt lịch ngay để nhận ưu đãi!");
            
            return objectMapper.writeValueAsString(mockResponse);
        } catch (Exception e) {
            log.error("Error creating mock response: {}", e.getMessage());
            return "{\"title\": \"Demo Title\", \"content\": \"Generated content\"}";
        }
    }
}
