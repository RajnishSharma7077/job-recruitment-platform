package com.jobplatform.dto;

public record AuthResponse(Long id, String name, String email, String role, String token) {
}
