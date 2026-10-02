package com.couplestory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> features;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "max_photos")
    private Integer maxPhotos;

    @Builder.Default
    @Column(name = "max_stories", nullable = false)
    private Integer maxStories = 1;

    @Builder.Default
    @Column(name = "allow_collaborator", nullable = false)
    private Boolean allowCollaborator = false;

    @Builder.Default
    @Column(name = "allow_custom_domain", nullable = false)
    private Boolean allowCustomDomain = false;

    @Column(name = "website_duration_days")
    private Integer websiteDurationDays;

    @Column(name = "max_total_stories")
    private Integer maxTotalStories;

    @Builder.Default
    @Column(name = "max_music_tracks", nullable = false)
    private Integer maxMusicTracks = 1;

    @Builder.Default
    @Column(name = "max_photos_per_event", nullable = false)
    private Integer maxPhotosPerEvent = 5;

    @Builder.Default
    @Column(name = "allow_password", nullable = false)
    private Boolean allowPassword = false;

    @Builder.Default
    @Column(name = "show_watermark", nullable = false)
    private Boolean showWatermark = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
