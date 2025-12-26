package org.uvhnael.fbadsbe2.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrendAnalysisScheduler {

    @Scheduled(cron = "0 0 6 * * 0") // Chạy vào chủ nhật hàng tuần lúc 6:00
    public void weeklyTrendAnalysis() {
        log.info("Running weekly trend analysis on Sunday");
    }
}
