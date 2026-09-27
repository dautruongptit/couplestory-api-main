package com.couplestory.repository;
import com.couplestory.entity.WebsiteCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteCollaboratorRepository extends JpaRepository<WebsiteCollaborator, String> {
    List<WebsiteCollaborator> findByWebsiteId(String websiteId);
    boolean existsByWebsiteIdAndUserIdAndStatus(String websiteId, String userId, String status);
    List<WebsiteCollaborator> findByWebsiteIdAndStatus(String websiteId, String status);
    Optional<WebsiteCollaborator> findByInviteToken(String token);
    List<WebsiteCollaborator> findByEmailAndStatus(String email, String status);
}
