package com.couplestory.controller;

import com.couplestory.entity.Plan;
import com.couplestory.entity.User;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.PlanRepository;
import com.couplestory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @GetMapping("/api/plans")
    public ResponseEntity<List<Plan>> getActivePlans() {
        return ResponseEntity.ok(planRepository.findByIsActiveTrueOrderBySortOrder());
    }

    @GetMapping("/api/plans/{code}")
    public ResponseEntity<Plan> getPlanByCode(@PathVariable String code) {
        Plan plan = planRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + code));
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/api/admin/plans")
    public ResponseEntity<List<Plan>> getAllPlans() {
        return ResponseEntity.ok(planRepository.findAll());
    }

    @PutMapping("/api/admin/plans/{id}")
    public ResponseEntity<Plan> updatePlan(@PathVariable UUID id, @RequestBody Plan update) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (update.getName() != null) plan.setName(update.getName());
        if (update.getPrice() != null) plan.setPrice(update.getPrice());
        if (update.getDescription() != null) plan.setDescription(update.getDescription());
        if (update.getFeatures() != null) plan.setFeatures(update.getFeatures());
        if (update.getSortOrder() != null) plan.setSortOrder(update.getSortOrder());
        if (update.getIsActive() != null) plan.setIsActive(update.getIsActive());
        if (update.getIsFeatured() != null) plan.setIsFeatured(update.getIsFeatured());
        if (update.getMaxPhotos() != null) plan.setMaxPhotos(update.getMaxPhotos());
        if (update.getMaxStories() != null) plan.setMaxStories(update.getMaxStories());
        if (update.getAllowCollaborator() != null) plan.setAllowCollaborator(update.getAllowCollaborator());
        if (update.getAllowCustomDomain() != null) plan.setAllowCustomDomain(update.getAllowCustomDomain());
        return ResponseEntity.ok(planRepository.save(plan));
    }

    @PostMapping("/api/admin/plans")
    public ResponseEntity<Plan> createPlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(planRepository.save(plan));
    }

    @PutMapping("/api/admin/users/{userId}/plan")
    public ResponseEntity<?> updateUserPlan(@PathVariable UUID userId, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPlanType(body.get("planType"));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Plan updated", "email", user.getEmail(), "plan", user.getPlanType()));
    }
}
