package org.uvhnael.fbadsbe2.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrendAnalysisScheduler {

    @Scheduled(cron = "0 0 6 * * *")
    public void dailyTrendAnalysis() {
        log.info("Running daily trend analysis (placeholder)");
    }
}
