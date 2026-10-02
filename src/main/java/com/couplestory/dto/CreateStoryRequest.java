package com.couplestory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStoryRequest {
    @NotBlank(message = "coupleName1 is required")
    @Size(max = 100)
    private String coupleName1;

    @NotBlank(message = "coupleName2 is required")
    @Size(max = 100)
    private String coupleName2;

    private String startDate; // ISO format yyyy-MM-dd

    @Size(max = 20)
    private String type; // LOVE_STORY or LOVE_CARD

    @Size(max = 50)
    private String templateCode; // e.g. 'eternal-love', 'minimal-couple'

    @Size(max = 255)
    private String title;

    @Size(max = 100)
    private String subdomain; // desired slug
}
