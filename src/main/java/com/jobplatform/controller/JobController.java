package com.jobplatform.controller;

import com.jobplatform.dto.ApplicationResponse;
import com.jobplatform.dto.JobRequest;
import com.jobplatform.dto.JobResponse;
import com.jobplatform.dto.PagedResponse;
import com.jobplatform.entity.Job;
import com.jobplatform.entity.Role;
import com.jobplatform.entity.User;
import com.jobplatform.service.ApplicationService;
import com.jobplatform.service.JobService;
import com.jobplatform.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final UserService userService;
    private final ApplicationService applicationService;

    public JobController(JobService jobService, UserService userService, ApplicationService applicationService) {
        this.jobService = jobService;
        this.userService = userService;
        this.applicationService = applicationService;
    }

    @GetMapping
    public PagedResponse<JobResponse> getAllJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "newest") @Pattern(regexp = "newest|salary|experience") String sort) {
        return PagedResponse.from(jobService.findAll(pageable(page, size, sort)), jobService::toResponse);
    }

    @GetMapping("/search")
    public PagedResponse<JobResponse> searchJobs(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String location,
                                                  @RequestParam(required = false) @Min(0) Integer minExperience,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                  @RequestParam(defaultValue = "newest") @Pattern(regexp = "newest|salary|experience") String sort) {
        return PagedResponse.from(
                jobService.findByFilters(keyword, location, minExperience, pageable(page, size, sort)),
                jobService::toResponse
        );
    }

    @GetMapping("/{jobId:\\d+}")
    public JobResponse getJob(@PathVariable Long jobId) {
        return jobService.toResponse(jobService.findById(jobId));
    }

    @GetMapping("/my")
    public PagedResponse<JobResponse> myJobs(Principal principal,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User recruiter = userService.findByEmail(principal.getName());
        return PagedResponse.from(jobService.findByRecruiter(recruiter, pageable(page, size, "newest")),
                jobService::toResponse);
    }

    @GetMapping("/{jobId}/applications")
    public PagedResponse<ApplicationResponse> getJobApplications(@PathVariable Long jobId,
                                                                  Principal principal,
                                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        User currentUser = userService.findByEmail(principal.getName());
        Job job = jobService.findById(jobId);

        if (currentUser.getRole() != Role.ADMIN && !job.getPostedBy().getEmail().equals(currentUser.getEmail())) {
            throw new IllegalArgumentException("You are not allowed to view this job's applications");
        }

        return PagedResponse.from(applicationService.findByJob(job, page, size), applicationService::toResponse);
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request, Principal principal) {
        User recruiter = userService.findByEmail(principal.getName());

        if (recruiter.getRole() != Role.RECRUITER && recruiter.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only recruiters and admins can create jobs");
        }

        return ResponseEntity.ok(jobService.toResponse(jobService.createJob(request, recruiter)));
    }

    private Pageable pageable(int page, int size, String sort) {
        Sort ordering = switch (sort) {
            case "experience" -> Sort.by(Sort.Order.asc("minExperience").nullsLast(), Sort.Order.desc("id"));
            case "salary" -> Sort.by(Sort.Order.desc("salarySortValue").nullsLast(), Sort.Order.desc("id"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            default -> throw new IllegalStateException("Validated job sort was not recognized");
        };
        return PageRequest.of(page, size, ordering);
    }
}
