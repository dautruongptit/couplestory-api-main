package com.couplestory.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebsiteEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "website_id", nullable = false)
    private String websiteId;

    @Column(nullable = false)
    private String title;

    private String description;
    
    @Column(name = "event_date")
    private LocalDate eventDate;
    
    @Column(name = "photo_id")
    private String photoId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Version
    private Integer version;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}