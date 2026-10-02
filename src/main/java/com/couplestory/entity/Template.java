package com.couplestory.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String type = "LOVE_STORY";

    @JsonProperty("package")
    @Builder.Default
    @Column(name = "package", nullable = false, length = 20)
    private String packageCode = "FREE";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "preview_image", length = 500)
    private String previewImage;

    @Builder.Default
    @Column(name = "recommended_events", nullable = false)
    private int recommendedEvents = 5;

    @Builder.Default
    @Column(name = "max_display_events", nullable = false)
    private int maxDisplayEvents = 6;

    @Builder.Default
    @Column(name = "min_events_for_publish", nullable = false)
    private int minEventsForPublish = 2;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
