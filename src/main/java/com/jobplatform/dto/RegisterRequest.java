package com.jobplatform.dto;

import com.jobplatform.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email,
        @NotBlank(message = "Password is required") String password,
        Role role
) {
}
