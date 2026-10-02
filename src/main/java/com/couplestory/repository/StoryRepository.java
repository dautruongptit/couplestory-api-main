package com.couplestory.repository;

import com.couplestory.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {
    List<Story> findByOwnerId(UUID ownerId);
    Optional<Story> findBySlug(String slug);
    long countByOwnerIdAndStatusNot(UUID ownerId, String status);
    long countByOwnerId(UUID ownerId);
    List<Story> findByStatusAndExpiresAtBefore(String status, java.time.OffsetDateTime time);
    Optional<Story> findBySlugAndStatus(String slug, String status);
    List<Story> findByOwnerIdAndStatusNot(UUID ownerId, String status);
}
