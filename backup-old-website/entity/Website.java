package com.couplestory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "websites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Website {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId; // Owner

    @Column(unique = true)
    private String subdomain;

    @Column(nullable = false)
    private String status; // draft, published, hidden, deleted

    @Column(name = "plan_type", nullable = false)
    private String planType; // TRIAL, PERMANENT, COUPLE, PRO

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_config")
    private String templateConfig;

    @Builder.Default
    @Column(name = "allow_partner_publish")
    private boolean allowPartnerPublish = false;

    @Column(name = "couple_name_1")
    private String coupleName1;

    @Column(name = "couple_name_2")
    private String coupleName2;

    private String title;

    @Column(name = "short_quote")
    private String shortQuote;

    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    @Column(name = "cover_photo_id")
    private String coverPhotoId;

    @Column(name = "owner_transferred_at")
    private LocalDateTime ownerTransferredAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "warning_sent_at")
    private LocalDateTime warningSentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Integer version;
}
