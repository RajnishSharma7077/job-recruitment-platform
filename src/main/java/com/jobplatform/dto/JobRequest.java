package com.jobplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JobRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Description is required") String description,
        @NotBlank(message = "Location is required") String location,
        @NotBlank(message = "Employment type is required") String employmentType,
        Integer minExperience,
        String salaryRange,
        @NotNull(message = "Company ID is required") Long companyId
) {
}
