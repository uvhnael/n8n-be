package org.uvhnael.fbadsbe2.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "insights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate reportDate;
    private Integer weekNumber;
    private Integer totalAds;
    private Integer imageCount;
    private Integer videoCount;
    private Integer carouselCount;
    private String dominantFormat;
    private java.math.BigDecimal ctaRate;
    private String mostActiveDay;

    @Column(columnDefinition = "TEXT")
    private String aiStrategyReport;

    private LocalDateTime createdAt;
}
