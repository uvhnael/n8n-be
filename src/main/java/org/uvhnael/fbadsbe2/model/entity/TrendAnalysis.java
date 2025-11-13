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

    @Column(columnDefinition = "JSON")
    private String trendingKeywords;

    @Column(columnDefinition = "JSON")
    private String trendingTopics;

    @Column(columnDefinition = "JSON")
    private String competitorActivity;

    @Column(columnDefinition = "JSON")
    private String contentSuggestions;

    @Column(columnDefinition = "JSON")
    private String optimalPostingTimes;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    private BigDecimal confidenceScore;

    private LocalDateTime createdAt;
}
