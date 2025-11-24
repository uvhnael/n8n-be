package org.uvhnael.fbadsbe2.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class containing reusable helper methods
 */
@Slf4j
public class Util {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Clean JSON response by removing markdown code blocks
     * @param response The raw response string that may contain markdown
     * @return Cleaned JSON string
     */
    public static String cleanJsonResponse(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }
        
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
     * Convert a List to JSON string
     * @param list The list to convert
     * @return JSON string representation or null if conversion fails
     */
    public static String convertListToJson(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON", e);
            return null;
        }
    }
    
    /**
     * Convert an Object to JSON string
     * @param object The object to convert
     * @return JSON string representation or null if conversion fails
     */
    public static String convertObjectToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            return null;
        }
    }
    
    /**
     * Check if text contains any of the specified keywords (case-insensitive)
     * @param text The text to search in
     * @param keywords The list of keywords to search for
     * @return true if any keyword is found, false otherwise
     */
    public static boolean containsAnyKeyword(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        return keywords.stream().anyMatch(lowerText::contains);
    }
    
    /**
     * Convert DayOfWeek to Vietnamese day name
     * @param dayOfWeek The DayOfWeek enum value
     * @return Vietnamese day name
     */
    public static String dayOfWeekToVietnamese(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return "UNKNOWN";
        }
        
        return switch (dayOfWeek) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ Nhật";
        };
    }
    
    /**
     * Get common Vietnamese stop words
     * @return Set of stop words to exclude from keyword extraction
     */
    public static Set<String> getVietnameseStopWords() {
        return Set.of(
            "và", "của", "có", "được", "này", "đó", "cho", "với", "từ", "trong",
            "là", "một", "các", "những", "đã", "sẽ", "để", "khi", "về", "hay",
            "hoặc", "nhưng", "không", "thì", "bởi", "nếu", "như", "rất", "đều",
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had"
        );
    }
    
    /**
     * Get CTA (Call-to-Action) keywords for Beauty industry
     * @return List of CTA keywords in Vietnamese and English
     */
    public static List<String> getBeautyCTAKeywords() {
        return Arrays.asList(
            // --- Nhóm Mua & Chốt đơn (Direct Sales) ---
            "mua ngay", "đặt hàng", "chốt đơn", "lên đơn",
            "shop now", "buy now", "order now", "add to cart",
            
            // --- Nhóm Đặt lịch & Giữ chỗ (Booking) ---
            "đặt lịch", "book lịch", "đặt hẹn", "book hẹn",
            "giữ chỗ", "giữ slot", "đặt suất", "lên lịch",
            "book now", "schedule", "appointment", "reservation",
            
            // --- Nhóm Tư vấn & Tương tác (Lead Gen) ---
            "tư vấn", "nhắn tin", "inbox", "gửi tin", "chat ngay", "gọi ngay", "liên hệ",
            "soi da", "thăm khám", "phác đồ", "báo giá",
            "ib", "cmt", "comment", "dr",
            "contact us", "call now", "message",
            
            // --- Nhóm Khuyến mãi & Trải nghiệm (Offer) ---
            "nhận ưu đãi", "săn deal", "lấy mã", "nhận voucher", "đăng ký ngay",
            "dùng thử", "trải nghiệm", "nhận quà", "khám phá",
            "sign up", "register", "claim offer", "get offer"
        );
    }
    
    /**
     * Extract keywords from text by filtering out stop words
     * @param text The text to extract keywords from
     * @param minLength Minimum keyword length (default: 3)
     * @return Array of extracted keywords
     */
    public static String[] extractKeywords(String text, int minLength) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        
        Set<String> stopWords = getVietnameseStopWords();
        
        return Arrays.stream(text
                .toLowerCase()
                .replaceAll("[^a-zA-ZÀ-ỹ0-9\\s]", " ")
                .split("\\s+"))
                .filter(word -> word.length() >= minLength && !stopWords.contains(word))
                .toArray(String[]::new);
    }
    
    /**
     * Extract keywords from text with default minimum length of 3
     * @param text The text to extract keywords from
     * @return Array of extracted keywords
     */
    public static String[] extractKeywords(String text) {
        return extractKeywords(text, 3);
    }
    
    /**
     * Safely get non-null value or default
     * @param value The value to check
     * @param defaultValue The default value if original is null
     * @return The value or default
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Check if string is null or empty
     * @param str The string to check
     * @return true if string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * Check if string is null or blank (empty or whitespace only)
     * @param str The string to check
     * @return true if string is null or blank
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }
}
