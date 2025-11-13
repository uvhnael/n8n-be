package org.uvhnael.fbadsbe2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.uvhnael.fbadsbe2.model.entity.GeneratedContent;
import org.uvhnael.fbadsbe2.model.entity.ScheduledPost;
import org.uvhnael.fbadsbe2.model.enums.PostStatus;
import org.uvhnael.fbadsbe2.repository.GeneratedContentRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostPublisherService {
    
    private final ScheduledPostService scheduledPostService;
    private final GeneratedContentRepository contentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${facebook.api.access-token:}")
    private String facebookAccessToken;
    
    @Value("${facebook.api.base-url:https://graph.facebook.com/v18.0}")
    private String facebookApiBaseUrl;

    /**
     * Publish post to the specified platform
     */
    @Transactional
    public void publish(ScheduledPost post) {
        log.info("Publishing post ID: {} to platform: {}", post.getId(), post.getPlatform());
        
        try {
            // Update status to PUBLISHING
            scheduledPostService.updatePostStatus(post.getId(), PostStatus.PUBLISHING, null, null);
            
            // Publish based on platform
            String postId = switch (post.getPlatform().toUpperCase()) {
                case "FACEBOOK" -> publishToFacebook(post);
                case "INSTAGRAM" -> publishToInstagram(post);
                default -> throw new UnsupportedOperationException(
                    "Platform not supported: " + post.getPlatform()
                );
            };
            
            // Update success
            scheduledPostService.updatePostStatus(post.getId(), PostStatus.PUBLISHED, postId, null);
            
            log.info("Successfully published post {} to {}. Platform post ID: {}", 
                post.getId(), post.getPlatform(), postId);
                
        } catch (Exception e) {
            log.error("Failed to publish post {}: {}", post.getId(), e.getMessage(), e);
            
            // Update failure
            scheduledPostService.updatePostStatus(
                post.getId(), 
                PostStatus.FAILED, 
                null, 
                e.getMessage()
            );
            
            // Retry logic if needed
            if (post.getRetryCount() < 3) {
                log.info("Will retry publishing post {} (attempt {} of 3)", 
                    post.getId(), post.getRetryCount() + 1);
                // TODO: Schedule retry after delay
            } else {
                log.error("Max retry attempts reached for post {}", post.getId());
            }
            
            throw e;
        }
    }

    /**
     * Publish to Facebook using Graph API
     */
    private String publishToFacebook(ScheduledPost post) {
        log.info("Publishing to Facebook page: {}", post.getPlatformPageId());
        
        // Get content
        GeneratedContent content = contentRepository.findById(post.getContentId())
            .orElseThrow(() -> new RuntimeException("Content not found"));
        
        // Check if access token is configured
        if (facebookAccessToken == null || facebookAccessToken.isEmpty()) {
            log.warn("Facebook access token not configured. Simulating successful publish.");
            return "SIMULATED_POST_ID_" + System.currentTimeMillis();
        }
        
        // Build request
        String url = String.format("%s/%s/feed", facebookApiBaseUrl, post.getPlatformPageId());
        
        Map<String, Object> request = new HashMap<>();
        request.put("message", content.getContent());
        request.put("access_token", facebookAccessToken);
        
        // Add media if present
        if (post.getMediaUrls() != null && !post.getMediaUrls().isEmpty()) {
            // TODO: Handle media upload
            // For now, just add as link
            log.info("Post has media URLs, but media upload not yet implemented");
        }
        
        try {
            // Make API call
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("id")) {
                return response.get("id").toString();
            } else {
                throw new RuntimeException("Failed to get post ID from Facebook response");
            }
        } catch (Exception e) {
            log.error("Facebook API error: {}", e.getMessage());
            throw new RuntimeException("Failed to publish to Facebook: " + e.getMessage(), e);
        }
    }

    /**
     * Publish to Instagram (placeholder for future implementation)
     */
    private String publishToInstagram(ScheduledPost post) {
        log.info("Publishing to Instagram (not yet implemented)");
        
        // TODO: Implement Instagram Publishing API
        // For now, simulate success
        log.warn("Instagram publishing not yet implemented. Simulating successful publish.");
        return "INSTAGRAM_SIMULATED_POST_ID_" + System.currentTimeMillis();
    }

    /**
     * Manually publish a post immediately (bypass schedule)
     */
    @Transactional
    public void publishNow(Long scheduledPostId) {
        log.info("Manual publish triggered for post ID: {}", scheduledPostId);
        
        ScheduledPost post = scheduledPostService.getScheduledPostById(scheduledPostId);
        
        // Validate post is in a publishable state
        if (!PostStatus.PENDING.name().equals(post.getStatus())) {
            throw new IllegalStateException("Cannot publish post with status: " + post.getStatus());
        }
        
        publish(post);
    }
}
