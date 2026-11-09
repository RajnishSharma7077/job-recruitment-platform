package com.jobplatform.controller;

import com.jobplatform.dto.AuthResponse;
import com.jobplatform.dto.LoginRequest;
import com.jobplatform.dto.RegisterRequest;
import com.jobplatform.entity.User;
import com.jobplatform.service.JwtService;
import com.jobplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.ok(toAuthResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(toAuthResponse(user));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                jwtService.generateToken(user));
    }
}
