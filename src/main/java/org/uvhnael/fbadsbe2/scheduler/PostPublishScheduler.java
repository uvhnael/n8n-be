package org.uvhnael.fbadsbe2.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.uvhnael.fbadsbe2.model.entity.ScheduledPost;
import org.uvhnael.fbadsbe2.service.PostPublisherService;
import org.uvhnael.fbadsbe2.service.ScheduledPostService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostPublishScheduler {

    private final ScheduledPostService scheduledPostService;
    private final PostPublisherService postPublisherService;

    /**
     * Check and publish posts every minute
     * Runs at the start of every minute (e.g., 10:00:00, 10:01:00, etc.)
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkAndPublishPosts() {
        log.info("Checking for posts to publish...");
        
        try {
            // Get posts scheduled within the next 5 minutes
            List<ScheduledPost> posts = scheduledPostService.getPostsToPublish();
            
            if (posts.isEmpty()) {
                log.debug("No posts to publish at this time");
                return;
            }
            
            log.info("Found {} post(s) to publish", posts.size());
            
            // Publish each post
            for (ScheduledPost post : posts) {
                try {
                    log.info("Publishing post ID: {} scheduled for {}", 
                        post.getId(), post.getScheduledTime());
                    
                    postPublisherService.publish(post);
                    
                    log.info("Successfully published post ID: {}", post.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to publish post ID {}: {}", 
                        post.getId(), e.getMessage(), e);
                    // Continue with next post even if this one fails
                }
            }
            
            log.info("Finished publishing {} post(s)", posts.size());
            
        } catch (Exception e) {
            log.error("Error in checkAndPublishPosts scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Health check log - runs every hour to confirm scheduler is active
     */
    @Scheduled(cron = "0 0 * * * *")
    public void schedulerHealthCheck() {
        log.info("PostPublishScheduler is running. Next check at top of next hour.");
    }
}
