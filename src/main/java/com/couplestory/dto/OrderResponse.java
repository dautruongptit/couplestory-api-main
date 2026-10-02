package com.couplestory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private String id;
    private String type;
    private String planCode;
    private String storyId;
    private String renewTerm;
    private long amount;
    private String transferCode;
    private String status;
    private String createdAt;
    private String paidAt;
    private String userEmail;      // admin listing only
    private String bankName;       // from configuration; empty when not configured
    private String accountNumber;
    private String accountName;
}
