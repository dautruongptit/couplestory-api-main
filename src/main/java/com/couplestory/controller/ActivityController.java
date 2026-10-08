package com.couplestory.controller;

import com.couplestory.entity.UserActivity;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.UserActivityService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/activity")
public class ActivityController {

    public record ActivityDto(UUID id, String action, String summary, String entityType, UUID entityId, OffsetDateTime createdAt) {
        static ActivityDto of(UserActivity a) {
            return new ActivityDto(a.getId(), a.getAction(), a.getSummary(), a.getEntityType(), a.getEntityId(), a.getCreatedAt());
        }
    }

    public record ActivityPage(List<ActivityDto> items, int page, int size, long total, boolean last) {}

    private final UserActivityService activityService;

    public ActivityController(UserActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<ActivityPage> list(@AuthenticationPrincipal UserDetailsImpl user,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Page<UserActivity> result = activityService.list(user.getId(), page, size);
        return ResponseEntity.ok(new ActivityPage(
                result.getContent().stream().map(ActivityDto::of).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.isLast()));
    }
}
