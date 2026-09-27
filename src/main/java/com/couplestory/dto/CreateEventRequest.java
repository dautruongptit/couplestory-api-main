package com.couplestory.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEventRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    private String eventDate; // ISO format yyyy-MM-dd
    private String photoId; // optional FK to photos
    private Integer order;
}
