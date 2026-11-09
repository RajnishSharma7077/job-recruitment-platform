package com.jobplatform.dto;

import java.time.LocalDateTime;

public record JobResponse(
        Long id,
        String title,
        String description,
        String location,
        String employmentType,
        Integer minExperience,
        String salaryRange,
        String companyName,
        String recruiterName,
        String status,
        LocalDateTime createdAt
) {
}
