package org.uvhnael.fbadsbe2.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private String contentType;
    private String platform;
    private String basedOnKeywords;
    private String basedOnTrends;

    private java.math.BigDecimal trendScore;

    private String aiModel;
    private String generationPrompt;
    private String status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
