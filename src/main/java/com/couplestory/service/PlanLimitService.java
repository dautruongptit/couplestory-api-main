package com.couplestory.service;

import com.couplestory.entity.Plan;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.PlanRepository;
import com.couplestory.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Resolves a user's plan row so limits (stories, photos, expiry...) come from the DB, not constants. */
@Service
public class PlanLimitService {
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public PlanLimitService(UserRepository userRepository, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    public String planCode(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getPlanType();
    }

    public Plan plan(UUID userId) {
        String code = planCode(userId);
        return planRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Plan not configured: " + code));
    }
}
