package com.jobplatform.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank(message = "Company name is required") String name,
        String description,
        String location,
        String website
) {
}
