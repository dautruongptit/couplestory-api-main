package com.couplestory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 150)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_quote", length = 500)
    private String shortQuote;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Builder.Default
    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType = "TRIAL";

    @Builder.Default
    @Column(name = "allow_partner_publish", nullable = false)
    private boolean allowPartnerPublish = false;

    @Column(name = "couple_name_1", nullable = false, length = 100)
    private String coupleName1;

    @Column(name = "couple_name_2", nullable = false, length = 100)
    private String coupleName2;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "cover_photo_id")
    private UUID coverPhotoId;

    @Builder.Default
    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode = "minimal-couple";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_config", nullable = false, columnDefinition = "jsonb")
    private String templateConfig = "{}";

    @Column(name = "owner_transferred_at")
    private OffsetDateTime ownerTransferredAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "trial_warning_sent_at")
    private OffsetDateTime trialWarningSentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Version
    private Long version;
}
