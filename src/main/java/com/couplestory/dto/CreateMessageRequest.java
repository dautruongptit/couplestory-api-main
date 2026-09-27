package com.couplestory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMessageRequest {
    @NotBlank(message = "type is required")
    @Size(max = 50)
    private String type; // 'love_letter' or 'final_message'

    @NotBlank(message = "content is required")
    @Size(max = 2000)
    private String content;

    @Size(max = 255)
    private String heading;

    @Size(max = 255)
    private String signature;
}
