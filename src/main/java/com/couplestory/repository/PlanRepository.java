package com.couplestory.repository;

import com.couplestory.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    Optional<Plan> findByCode(String code);

    List<Plan> findByIsActiveTrueOrderBySortOrder();
}
