package org.uvhnael.fbadsbe2.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduledPostDTO {
    private Long contentId;
    private String platform;
    private String platformPageId;
    private LocalDateTime scheduledTime;
    private String postType;
    private List<String> mediaUrls;
    private List<String> hashtags;
    private String callToAction;
    private Boolean autoPublish;
}
