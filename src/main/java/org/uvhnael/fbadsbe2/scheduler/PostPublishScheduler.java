package org.uvhnael.fbadsbe2.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostPublishScheduler {

    // Placeholders for services - can be injected when implemented

    @Scheduled(cron = "0 * * * * *")
    public void checkAndPublishPosts() {
        log.info("Checking for posts to publish (placeholder)");
    }
}
