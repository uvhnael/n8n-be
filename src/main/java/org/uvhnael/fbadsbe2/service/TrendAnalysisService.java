package org.uvhnael.fbadsbe2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.uvhnael.fbadsbe2.exception.CustomExceptions.NotFoundException;
import org.uvhnael.fbadsbe2.model.entity.TrendAnalysis;
import org.uvhnael.fbadsbe2.repository.TrendAnalysisRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendAnalysisService {
    
    private final TrendAnalysisRepository trendAnalysisRepository;

    /**
     * Get the most recent trend analysis
     */
    public TrendAnalysis getLatestTrend() {
        return trendAnalysisRepository.findTopByOrderByCreatedAtDesc()
            .orElseThrow(() -> new NotFoundException("No trend analysis found"));
    }

    /**
     * Get all trend analyses
     */
    public List<TrendAnalysis> getAllTrends() {
        return trendAnalysisRepository.findAll();
    }

    /**
     * Get trend by ID
     */
    public TrendAnalysis getTrendById(Long id) {
        return trendAnalysisRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Trend analysis not found with ID: " + id));
    }

    /**
     * Get trend analysis for a specific date
     */
    public Optional<TrendAnalysis> getTrendsByDate(LocalDate date) {
        return trendAnalysisRepository.findByAnalysisDate(date);
    }

    /**
     * Get the most recent trend analysis (backward compatibility)
     */
    public TrendAnalysis getCurrentTrends() {
        try {
            return getLatestTrend();
        } catch (NotFoundException e) {
            // Return a default/mock trend analysis if none exists
            log.warn("No trend analysis found, returning default");
            return createDefaultTrends();
        }
    }

    /**
     * Create default trend analysis (placeholder)
     */
    private TrendAnalysis createDefaultTrends() {
        return TrendAnalysis.builder()
            .analysisDate(LocalDate.now())
            .trendingKeywords("[\"giảm giá\", \"spa\", \"làm đẹp\", \"chăm sóc da\", \"ưu đãi\"]")
            .trendingTopics("[\"Ưu đãi cuối tuần\", \"Chăm sóc da mùa đông\"]")
            .contentSuggestions("[\"Bài viết về xu hướng skincare mùa đông\", \"Video hướng dẫn massage thư giãn\"]")
            .aiSummary("Current trends show interest in spa services and beauty care")
            .build();
    }
}
