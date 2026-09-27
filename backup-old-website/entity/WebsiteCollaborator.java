package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_collaborators")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteCollaborator {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;

    @Column(name = "user_id")
    private String userId; // NULLABLE

    @Column(nullable = false)
    private String email;

    @Column
    private String role; // PARTNER

    @Column(nullable = false)
    private String status; // pending, accepted, removed

    @Column(name = "invite_token")
    private String inviteToken;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}