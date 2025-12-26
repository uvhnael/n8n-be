package org.uvhnael.fbadsbe2.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class ScheduledPostDTO {
    private Long contentId;
    private String platform;
    private String platformPageId;
    private String scheduledTime; // ISO 8601 format: "2025-12-26T21:02:00.000Z"
    private String postType;
    private List<String> mediaUrls;
    private List<String> hashtags;
    private String callToAction;
    private Boolean autoPublish;
}
