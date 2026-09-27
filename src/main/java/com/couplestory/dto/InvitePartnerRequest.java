package com.couplestory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvitePartnerRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;
}
