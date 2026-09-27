package com.couplestory.repository;
import com.couplestory.entity.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteRepository extends JpaRepository<Website, String> {
    List<Website> findByUserId(String userId);
    Optional<Website> findBySubdomain(String subdomain);
    long countByUserIdAndStatusNot(String userId, String status);
    Optional<Website> findBySubdomainAndStatus(String subdomain, String status);
    List<Website> findByUserIdAndStatusNot(String userId, String status);
}
