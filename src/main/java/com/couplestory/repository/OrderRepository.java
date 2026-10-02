package com.couplestory.repository;

import com.couplestory.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByStatusOrderByCreatedAtAsc(String status);

    boolean existsByTransferCode(String transferCode);

    Optional<Order> findByUserIdAndTypeAndPlanCodeAndStatus(UUID userId, String type, String planCode, String status);

    Optional<Order> findByUserIdAndTypeAndStoryIdAndRenewTermAndStatus(UUID userId, String type, UUID storyId, String renewTerm, String status);
}
