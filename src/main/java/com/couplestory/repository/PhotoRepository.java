package com.couplestory.repository;

import com.couplestory.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    List<Photo> findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(String ownerType, UUID ownerId);
    long countByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
    List<Photo> findByOwnerTypeAndOwnerIdInOrderBySortOrderAsc(String ownerType, java.util.Collection<UUID> ownerIds);
}
