package org.uvhnael.fbadsbe2.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdDTO {
    private Long id;
    private String adArchiveId;
    private String pageName;
    private String caption;
    private String typeAds;
    private String urlAdsPost;
    private String aiAnalyze;
    private String imgUrl;
    private String videoUrl;
    private String status;
    private LocalDate timeCreated;
    private LocalDateTime scrapedAt;
}
