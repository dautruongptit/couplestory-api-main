package com.couplestory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoryResponse {
    private String id;
    private String slug;
    private String status;
    private String planType;
    private String templateCode;
    private String coupleName1;
    private String coupleName2;
    private String title;
    private String shortQuote;
    private String description;
    private String startDate;
    private String coverPhotoId;
    private String createdAt;
    private String publishedAt;
    private String expiresAt;
}
