package com.couplestory.controller;

import com.couplestory.dto.CreateOrderRequest;
import com.couplestory.dto.OrderResponse;
import com.couplestory.entity.Order;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class OrderController {
    private final OrderService orderService;
    private final String bankName;
    private final String accountNumber;
    private final String accountName;

    public OrderController(OrderService orderService,
                           @Value("${app.payment.bank-name:}") String bankName,
                           @Value("${app.payment.account-number:}") String accountNumber,
                           @Value("${app.payment.account-name:}") String accountName) {
        this.orderService = orderService;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
    }

    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request,
                                                @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(toResponse(orderService.create(user.getId(), request), null));
    }

    @GetMapping("/api/orders")
    public ResponseEntity<List<OrderResponse>> mine(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(orderService.listMine(user.getId()).stream().map(o -> toResponse(o, null)).toList());
    }

    @GetMapping("/api/orders/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id, @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(toResponse(orderService.getMine(user.getId(), id), null));
    }

    @PostMapping("/api/orders/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID id, @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(toResponse(orderService.cancel(user.getId(), id), null));
    }

    @GetMapping("/api/admin/orders")
    public ResponseEntity<List<OrderResponse>> adminList(@RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(orderService.listByStatus(status).stream()
                .map(o -> toResponse(o, orderService.emailOf(o.getUserId()))).toList());
    }

    @PostMapping("/api/admin/orders/{id}/confirm")
    public ResponseEntity<OrderResponse> adminConfirm(@PathVariable UUID id, @AuthenticationPrincipal UserDetailsImpl admin) {
        Order order = orderService.confirm(id, admin.getId());
        return ResponseEntity.ok(toResponse(order, orderService.emailOf(order.getUserId())));
    }

    private OrderResponse toResponse(Order o, String userEmail) {
        return OrderResponse.builder()
                .id(o.getId().toString())
                .type(o.getType())
                .planCode(o.getPlanCode())
                .storyId(o.getStoryId() != null ? o.getStoryId().toString() : null)
                .renewTerm(o.getRenewTerm())
                .amount(o.getAmount())
                .transferCode(o.getTransferCode())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt() != null ? o.getCreatedAt().toString() : null)
                .paidAt(o.getPaidAt() != null ? o.getPaidAt().toString() : null)
                .userEmail(userEmail)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .build();
    }
}
