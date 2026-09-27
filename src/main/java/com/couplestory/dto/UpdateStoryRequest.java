package com.couplestory.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStoryRequest {
    @Size(max = 100)
    private String coupleName1;

    @Size(max = 100)
    private String coupleName2;

    private String startDate;

    @Size(max = 100)
    private String subdomain;

    private String coverPhotoId;

    @Size(max = 255)
    private String title;

    @Size(max = 500)
    private String shortQuote;
}
