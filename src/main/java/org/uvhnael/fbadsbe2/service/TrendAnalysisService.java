package org.uvhnael.fbadsbe2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.uvhnael.fbadsbe2.model.entity.TrendAnalysis;
import org.uvhnael.fbadsbe2.repository.TrendAnalysisRepository;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendAnalysisService {
    
    private final TrendAnalysisRepository trendAnalysisRepository;

    /**
     * Get the most recent trend analysis
     */
    public TrendAnalysis getCurrentTrends() {
        Optional<TrendAnalysis> latest = trendAnalysisRepository.findTopByOrderByCreatedAtDesc();
        
        if (latest.isPresent()) {
            return latest.get();
        }
        
        // Return a default/mock trend analysis if none exists
        log.warn("No trend analysis found, returning default");
        return createDefaultTrends();
    }

    /**
     * Get trend analysis for a specific date
     */
    public Optional<TrendAnalysis> getTrendsByDate(LocalDate date) {
        return trendAnalysisRepository.findByAnalysisDate(date);
    }

    /**
     * Create default trend analysis (placeholder)
     */
    private TrendAnalysis createDefaultTrends() {
        return TrendAnalysis.builder()
            .analysisDate(LocalDate.now())
            .trendingKeywords("[\"giảm giá\", \"spa\", \"làm đẹp\", \"chăm sóc da\", \"ưu đãi\"]")
            .trendingTopics("[\"Ưu đãi cuối tuần\", \"Chăm sóc da mùa đông\"]")
            .aiSummary("Current trends show interest in spa services and beauty care")
            .build();
    }
}
