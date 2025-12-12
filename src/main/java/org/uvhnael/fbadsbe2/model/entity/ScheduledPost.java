package org.uvhnael.fbadsbe2.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contentId;
    private String platform;
    private String platformPageId;
    private LocalDateTime scheduledTime;
    private String postType;
    private String mediaUrls;
    private String hashtags;
    private String callToAction;

    private String status;
    private LocalDateTime publishedAt;
    private String publishError;
    private Integer retryCount;

    private String postId;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer sharesCount;
    private Integer reach;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
