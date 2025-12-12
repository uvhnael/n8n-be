package org.uvhnael.fbadsbe2.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trend_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate analysisDate;
    private String trendingKeywords;
    private String trendingTopics;
    private String competitorActivity;
    private String contentSuggestions;
    private String optimalPostingTimes;
    private String aiSummary;

    private BigDecimal confidenceScore;

    private LocalDateTime createdAt;
}
