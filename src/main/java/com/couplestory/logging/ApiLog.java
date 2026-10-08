package com.couplestory.logging;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 10)
    private String method;

    /** Route template (/api/stories/{id}), never the raw URL: no query strings, no identifiers. */
    @Column(nullable = false, length = 200)
    private String route;

    @Column(nullable = false)
    private short status;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
