package com.couplestory.service;

import com.couplestory.dto.CreateOrderRequest;
import com.couplestory.entity.Order;
import com.couplestory.entity.Plan;
import com.couplestory.entity.Story;
import com.couplestory.entity.User;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.OrderRepository;
import com.couplestory.repository.PlanRepository;
import com.couplestory.repository.StoryRepository;
import com.couplestory.repository.UserRepository;
import com.couplestory.util.PackageRank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Manual-payment orders: the user transfers money with a transfer code, an admin confirms. */
@Service
public class OrderService {
    static final long RENEW_3_MONTHS = 19_000;
    static final long RENEW_1_YEAR = 29_000;

    private final OrderRepository orderRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final StoryAccessService storyAccessService;

    public OrderService(OrderRepository orderRepository, PlanRepository planRepository,
                        UserRepository userRepository, StoryRepository storyRepository,
                        StoryAccessService storyAccessService) {
        this.orderRepository = orderRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.storyRepository = storyRepository;
        this.storyAccessService = storyAccessService;
    }

    @Transactional
    public Order create(UUID userId, CreateOrderRequest request) {
        if ("UPGRADE".equals(request.getType())) return createUpgrade(userId, request.getPlanCode());
        if ("RENEW".equals(request.getType())) return createRenewal(userId, request.getStoryId(), request.getTerm());
        throw new IllegalArgumentException("type must be UPGRADE or RENEW");
    }

    private Order createUpgrade(UUID userId, String planCode) {
        if (planCode == null) throw new IllegalArgumentException("planCode is required");
        User user = requireUser(userId);
        Plan target = planRepository.findByCode(planCode)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("Gói không tồn tại: " + planCode));
        if (PackageRank.of(target.getCode()) <= PackageRank.of(user.getPlanType())) {
            throw new IllegalArgumentException("Chỉ có thể nâng lên gói cao hơn gói hiện tại (" + user.getPlanType() + ").");
        }
        var pending = orderRepository.findByUserIdAndTypeAndPlanCodeAndStatus(userId, "UPGRADE", planCode, "PENDING");
        if (pending.isPresent()) return pending.get();

        // Pay the difference between the current plan and the target.
        long currentPrice = planRepository.findByCode(user.getPlanType()).map(Plan::getPrice).orElse(0L);
        long amount = Math.max(0, target.getPrice() - currentPrice);
        return orderRepository.save(Order.builder()
                .userId(userId).type("UPGRADE").planCode(planCode)
                .amount(amount).transferCode(newTransferCode()).build());
    }

    private Order createRenewal(UUID userId, UUID storyId, String term) {
        if (storyId == null) throw new IllegalArgumentException("storyId is required");
        if (!"3M".equals(term) && !"1Y".equals(term)) throw new IllegalArgumentException("term must be 3M or 1Y");
        Story story = storyAccessService.requireOwnedStory(storyId, userId);
        if ("DELETED".equals(story.getStatus())) throw new IllegalArgumentException("Website đã bị xóa.");
        User user = requireUser(userId);
        if ("PREMIUM".equals(user.getPlanType())) {
            throw new IllegalArgumentException("Gói PREMIUM có website vĩnh viễn, không cần gia hạn.");
        }
        var pending = orderRepository.findByUserIdAndTypeAndStoryIdAndRenewTermAndStatus(userId, "RENEW", storyId, term, "PENDING");
        if (pending.isPresent()) return pending.get();

        return orderRepository.save(Order.builder()
                .userId(userId).type("RENEW").storyId(storyId).renewTerm(term)
                .amount("3M".equals(term) ? RENEW_3_MONTHS : RENEW_1_YEAR)
                .transferCode(newTransferCode()).build());
    }

    public List<Order> listMine(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getMine(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) throw new ForbiddenOperationException("Order does not belong to you");
        return order;
    }

    @Transactional
    public Order cancel(UUID userId, UUID orderId) {
        Order order = getMine(userId, orderId);
        if (!"PENDING".equals(order.getStatus())) throw new IllegalArgumentException("Chỉ hủy được đơn đang chờ thanh toán.");
        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    public List<Order> listByStatus(String status) {
        return orderRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    public String emailOf(UUID userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }

    @Transactional
    public Order confirm(UUID orderId, UUID adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!"PENDING".equals(order.getStatus())) throw new IllegalArgumentException("Đơn này không ở trạng thái chờ thanh toán.");

        if ("UPGRADE".equals(order.getType())) {
            User user = requireUser(order.getUserId());
            // Never downgrade: if the user already reached this plan another way, only mark the order paid.
            if (PackageRank.of(order.getPlanCode()) > PackageRank.of(user.getPlanType())) {
                user.setPlanType(order.getPlanCode());
                userRepository.save(user);
            }
        } else {
            Story story = storyRepository.findById(order.getStoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime base = story.getExpiresAt() != null && story.getExpiresAt().isAfter(now) ? story.getExpiresAt() : now;
            story.setExpiresAt("3M".equals(order.getRenewTerm()) ? base.plusMonths(3) : base.plusYears(1));
            if ("HIDDEN".equals(story.getStatus())) story.setStatus("PUBLISHED");
            storyRepository.save(story);
        }

        order.setStatus("PAID");
        order.setPaidAt(OffsetDateTime.now());
        order.setConfirmedBy(adminId);
        return orderRepository.save(order);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String newTransferCode() {
        for (int i = 0; i < 5; i++) {
            String code = "CS" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            if (!orderRepository.existsByTransferCode(code)) return code;
        }
        throw new IllegalStateException("Could not generate a unique transfer code");
    }
}
