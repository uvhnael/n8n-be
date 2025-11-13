package org.uvhnael.fbadsbe2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uvhnael.fbadsbe2.exception.CustomExceptions.NotFoundException;
import org.uvhnael.fbadsbe2.exception.CustomExceptions.ValidationException;
import org.uvhnael.fbadsbe2.model.dto.ScheduledPostDTO;
import org.uvhnael.fbadsbe2.model.entity.GeneratedContent;
import org.uvhnael.fbadsbe2.model.entity.PublishHistory;
import org.uvhnael.fbadsbe2.model.entity.ScheduledPost;
import org.uvhnael.fbadsbe2.model.enums.ContentStatus;
import org.uvhnael.fbadsbe2.model.enums.PostStatus;
import org.uvhnael.fbadsbe2.repository.GeneratedContentRepository;
import org.uvhnael.fbadsbe2.repository.PublishHistoryRepository;
import org.uvhnael.fbadsbe2.repository.ScheduledPostRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledPostService {
    
    private final ScheduledPostRepository scheduledPostRepository;
    private final GeneratedContentRepository contentRepository;
    private final PublishHistoryRepository publishHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Schedule a new post
     */
    @Transactional
    public ScheduledPost schedulePost(ScheduledPostDTO dto) {
        log.info("Scheduling post for content ID: {}", dto.getContentId());
        
        // Validate content exists
        GeneratedContent content = contentRepository.findById(dto.getContentId())
            .orElseThrow(() -> new NotFoundException("Content not found with ID: " + dto.getContentId()));
        
        // Check content is approved
        if (!ContentStatus.APPROVED.name().equals(content.getStatus())) {
            throw new ValidationException("Content must be approved before scheduling. Current status: " + content.getStatus());
        }
        
        // Validate scheduled time is in the future
        if (dto.getScheduledTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Scheduled time must be in the future");
        }
        
        // Create scheduled post
        ScheduledPost post = ScheduledPost.builder()
            .contentId(dto.getContentId())
            .platform(dto.getPlatform())
            .platformPageId(dto.getPlatformPageId())
            .scheduledTime(dto.getScheduledTime())
            .postType(dto.getPostType())
            .mediaUrls(convertListToJson(dto.getMediaUrls()))
            .hashtags(convertListToJson(dto.getHashtags()))
            .callToAction(dto.getCallToAction())
            .status(PostStatus.PENDING.name())
            .retryCount(0)
            .likesCount(0)
            .commentsCount(0)
            .sharesCount(0)
            .reach(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        post = scheduledPostRepository.save(post);
        
        // Log history
        logHistory(post.getId(), "CREATED", PostStatus.PENDING.name(), 
            "Post scheduled successfully for " + dto.getScheduledTime());
        
        log.info("Post scheduled successfully with ID: {}", post.getId());
        return post;
    }

    /**
     * Get posts that need to be published within the next 5 minutes
     */
    public List<ScheduledPost> getPostsToPublish() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next5Min = now.plusMinutes(5);
        
        return scheduledPostRepository.findByScheduledTimeBetweenAndStatus(
            now, next5Min, PostStatus.PENDING.name()
        );
    }

    /**
     * Get all scheduled posts with optional filters
     */
    public List<ScheduledPost> getAllScheduledPosts() {
        return scheduledPostRepository.findAll();
    }

    /**
     * Get a single scheduled post by ID
     */
    public ScheduledPost getScheduledPostById(Long id) {
        return scheduledPostRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Scheduled post not found with ID: " + id));
    }

    /**
     * Update a scheduled post
     */
    @Transactional
    public ScheduledPost updateScheduledPost(Long id, ScheduledPostDTO dto) {
        log.info("Updating scheduled post ID: {}", id);
        
        ScheduledPost post = getScheduledPostById(id);
        
        // Only allow updates if post is still pending
        if (!PostStatus.PENDING.name().equals(post.getStatus())) {
            throw new ValidationException("Cannot update post with status: " + post.getStatus());
        }
        
        // Update fields
        if (dto.getScheduledTime() != null) {
            if (dto.getScheduledTime().isBefore(LocalDateTime.now())) {
                throw new ValidationException("Scheduled time must be in the future");
            }
            post.setScheduledTime(dto.getScheduledTime());
        }
        
        if (dto.getPlatform() != null) post.setPlatform(dto.getPlatform());
        if (dto.getPlatformPageId() != null) post.setPlatformPageId(dto.getPlatformPageId());
        if (dto.getPostType() != null) post.setPostType(dto.getPostType());
        if (dto.getMediaUrls() != null) post.setMediaUrls(convertListToJson(dto.getMediaUrls()));
        if (dto.getHashtags() != null) post.setHashtags(convertListToJson(dto.getHashtags()));
        if (dto.getCallToAction() != null) post.setCallToAction(dto.getCallToAction());
        
        post.setUpdatedAt(LocalDateTime.now());
        post = scheduledPostRepository.save(post);
        
        logHistory(post.getId(), "UPDATED", post.getStatus(), "Post updated successfully");
        
        log.info("Post updated successfully");
        return post;
    }

    /**
     * Cancel a scheduled post
     */
    @Transactional
    public void cancelScheduledPost(Long id) {
        log.info("Cancelling scheduled post ID: {}", id);
        
        ScheduledPost post = getScheduledPostById(id);
        
        // Only allow cancellation if post is pending
        if (!PostStatus.PENDING.name().equals(post.getStatus())) {
            throw new ValidationException("Cannot cancel post with status: " + post.getStatus());
        }
        
        post.setStatus(PostStatus.CANCELLED.name());
        post.setUpdatedAt(LocalDateTime.now());
        scheduledPostRepository.save(post);
        
        logHistory(post.getId(), "CANCELLED", PostStatus.CANCELLED.name(), "Post cancelled by user");
        
        log.info("Post cancelled successfully");
    }

    /**
     * Update post status after publishing attempt
     */
    @Transactional
    public void updatePostStatus(Long id, PostStatus status, String postId, String error) {
        ScheduledPost post = getScheduledPostById(id);
        
        post.setStatus(status.name());
        post.setUpdatedAt(LocalDateTime.now());
        
        if (status == PostStatus.PUBLISHED) {
            post.setPostId(postId);
            post.setPublishedAt(LocalDateTime.now());
            logHistory(id, "PUBLISHED", status.name(), "Post published successfully. Platform post ID: " + postId);
        } else if (status == PostStatus.FAILED) {
            post.setPublishError(error);
            post.setRetryCount(post.getRetryCount() + 1);
            logHistory(id, "FAILED", status.name(), "Publish failed: " + error);
        }
        
        scheduledPostRepository.save(post);
    }

    /**
     * Log history for a scheduled post
     */
    private void logHistory(Long scheduledPostId, String action, String status, String message) {
        PublishHistory history = PublishHistory.builder()
            .scheduledPostId(scheduledPostId)
            .action(action)
            .status(status)
            .message(message)
            .createdAt(LocalDateTime.now())
            .build();
        
        publishHistoryRepository.save(history);
    }

    /**
     * Helper method to convert list to JSON string
     */
    private String convertListToJson(List<String> list) {
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
}
