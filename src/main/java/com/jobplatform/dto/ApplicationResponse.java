package com.jobplatform.dto;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        Long jobId,
        String jobTitle,
        String companyName,
        String resumeUrl,
        String status,
        LocalDateTime appliedAt
) {
}
