package com.couplestory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "plan_code", length = 20)
    private String planCode;

    @Column(name = "story_id")
    private UUID storyId;

    @Column(name = "renew_term", length = 10)
    private String renewTerm;

    @Column(nullable = false)
    private long amount;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 20)
    private String transferCode;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;
}
