package org.uvhnael.fbadsbe2.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad_archive_id", unique = true, nullable = false)
    private String adArchiveId;

    private String pageName;

    @Column(columnDefinition = "TEXT")
    private String caption;

    private String typeAds;
    private String urlAdsPost;

    @Column(columnDefinition = "TEXT")
    private String aiAnalyze;

    private String imgUrl;
    private String videoUrl;

    private String status;

    private LocalDate timeCreated;

    private LocalDateTime scrapedAt;
}
