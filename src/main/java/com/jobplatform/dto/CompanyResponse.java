package com.jobplatform.dto;

public record CompanyResponse(
        Long id,
        String name,
        String description,
        String location,
        String website
) {
}
