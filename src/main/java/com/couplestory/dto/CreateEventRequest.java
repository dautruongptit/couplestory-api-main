package com.couplestory.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEventRequest {
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String message;

    @Size(max = 150)
    private String location;

    private String eventDate; // optional, ISO format yyyy-MM-dd
    private String photoId; // optional FK to photos
    private Integer order;
}
