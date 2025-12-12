package org.uvhnael.fbadsbe2.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "publish_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scheduledPostId;
    private String action;
    private String status;
    private String message;
    private String metadata;
    private LocalDateTime createdAt;
}
