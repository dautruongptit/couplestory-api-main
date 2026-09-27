package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;
    
    @Column(nullable = false)
    private String type;

    private String heading;

    private String signature;

    @Column(nullable = false, length = 2000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}