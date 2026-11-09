package com.jobplatform.service;

import com.jobplatform.entity.Role;
import com.jobplatform.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-signing-key-with-at-least-32-bytes-long",
            60_000
    );

    @Test
    void generatedTokenContainsTheUsersEmail() {
        User user = new User("Candidate", "candidate@example.com", "hashed-password", Role.CANDIDATE);

        String token = jwtService.generateToken(user);

        assertEquals("candidate@example.com", jwtService.extractUsername(token));
    }

    @Test
    void rejectsTamperedToken() {
        User user = new User("Candidate", "candidate@example.com", "hashed-password", Role.CANDIDATE);
        String token = jwtService.generateToken(user);

        JwtException exception = assertThrows(
                JwtException.class,
                () -> jwtService.extractUsername(token + "tampered")
        );
        assertNotNull(exception.getMessage());
    }
}
