package com.couplestory.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMomentRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    private String photoId;
    private Integer order;
}
