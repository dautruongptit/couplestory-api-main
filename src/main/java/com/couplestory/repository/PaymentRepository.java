package com.couplestory.repository;

import com.couplestory.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByStoryId(UUID storyId);
    Optional<Payment> findByTransactionRef(String transactionRef);
}
