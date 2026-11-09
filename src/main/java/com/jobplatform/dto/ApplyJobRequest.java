package com.jobplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplyJobRequest(
        @NotNull(message = "Job ID is required") Long jobId,
        @NotBlank(message = "Resume URL is required") String resumeUrl
) {
}
