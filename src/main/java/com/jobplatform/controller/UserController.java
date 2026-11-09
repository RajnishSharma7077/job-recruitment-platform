package com.jobplatform.controller;

import com.jobplatform.dto.UserResponse;
import com.jobplatform.entity.User;
import com.jobplatform.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        return userService.toResponse(user);
    }
}
