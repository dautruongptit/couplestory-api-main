package com.couplestory.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateOrderRequest {
    private String type;      // UPGRADE | RENEW
    private String planCode;  // UPGRADE: target plan
    private UUID storyId;     // RENEW
    private String term;      // RENEW: 3M | 1Y
}
