package com.couplestory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "story_music")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    @Column(name = "track_id", nullable = false)
    private UUID trackId;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
