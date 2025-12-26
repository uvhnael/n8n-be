package org.uvhnael.fbadsbe2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.uvhnael.fbadsbe2.model.dto.ScheduledPostDTO;
import org.uvhnael.fbadsbe2.model.entity.GeneratedContent;
import org.uvhnael.fbadsbe2.repository.GeneratedContentRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostPublisherService {
    
    private final GeneratedContentRepository contentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${n8n.webhook.url:}")
    private String n8nWebhookUrl;

    /**
     * Schedule a post by triggering n8n webhook (replaces database scheduling)
     */
    public void schedulePostViaN8n(ScheduledPostDTO dto) {
        log.info("Scheduling post via n8n webhook for content ID: {}", dto.getContentId());
        
        // Validate content exists and is approved
        GeneratedContent content = contentRepository.findById(dto.getContentId())
            .orElseThrow(() -> new RuntimeException("Content not found with ID: " + dto.getContentId()));
        
        if (!"APPROVED".equals(content.getStatus())) {
            throw new RuntimeException("Content must be approved before scheduling. Current status: " + content.getStatus());
        }
        
        // Validate scheduled time is in the future
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        ZonedDateTime scheduledZoned = dto.getScheduledTime().atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        
        if (scheduledZoned.isBefore(now)) {
            throw new RuntimeException("Scheduled time must be in the future. Current time: " + now + ", scheduled: " + scheduledZoned);
        }
        
        // Check if webhook URL is configured
        if (n8nWebhookUrl == null || n8nWebhookUrl.isEmpty()) {
            log.warn("n8n webhook URL not configured. Cannot schedule post.");
            throw new RuntimeException("n8n webhook URL not configured");
        }
        
        // Build webhook payload for scheduling
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "schedule");
        payload.put("contentId", dto.getContentId());
        payload.put("platform", dto.getPlatform());
        payload.put("platformPageId", dto.getPlatformPageId());
        payload.put("scheduledTime", dto.getScheduledTime());
        payload.put("postType", dto.getPostType());
        payload.put("mediaUrls", dto.getMediaUrls());
        payload.put("hashtags", dto.getHashtags());
        payload.put("callToAction", dto.getCallToAction());
        payload.put("content", content);
        payload.put("title", content.getTitle());
        payload.put("imagePrompt", content.getImagePrompt());
        
        try {
            // Make webhook call
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(n8nWebhookUrl, payload, Map.class);
            
            log.info("Successfully triggered n8n webhook for scheduling post at {}", dto.getScheduledTime());
            
        } catch (Exception e) {
            log.error("Failed to trigger n8n webhook for scheduling: {}", e.getMessage());
            throw new RuntimeException("Failed to schedule post via n8n: " + e.getMessage(), e);
        }
    }
}
