package com.jobplatform.controller;

import com.jobplatform.dto.ApplicationResponse;
import com.jobplatform.dto.ApplyJobRequest;
import com.jobplatform.dto.PagedResponse;
import com.jobplatform.entity.JobApplication;
import com.jobplatform.entity.Role;
import com.jobplatform.entity.User;
import com.jobplatform.service.ApplicationService;
import com.jobplatform.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    public ApplicationController(ApplicationService applicationService, UserService userService) {
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplicationResponse> applyForJob(@Valid @RequestBody ApplyJobRequest request,
                                                          Principal principal) {
        User candidate = userService.findByEmail(principal.getName());
        JobApplication application = applicationService.applyToJob(request.jobId(), candidate, request.resumeUrl());
        return ResponseEntity.ok(applicationService.toResponse(application));
    }

    @GetMapping("/mine")
    public PagedResponse<ApplicationResponse> myApplications(
            Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User candidate = userService.findByEmail(principal.getName());
        Pageable pageable = applicationService.applicationPage(page, size);
        return PagedResponse.from(
                applicationService.findByCandidate(candidate, pageable),
                applicationService::toResponse
        );
    }

    @GetMapping("/recruiter/me")
    public PagedResponse<ApplicationResponse> recruiterApplications(
            Principal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User currentUser = userService.findByEmail(principal.getName());
        if (currentUser.getRole() != Role.RECRUITER && currentUser.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only recruiters and admins can view recruiter applications");
        }

        Pageable pageable = applicationService.applicationPage(page, size);
        return PagedResponse.from(
                applicationService.findByRecruiter(currentUser, pageable),
                applicationService::toResponse
        );
    }
}
