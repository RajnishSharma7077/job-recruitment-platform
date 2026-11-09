package com.jobplatform.controller;

import com.jobplatform.dto.DemoProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final List<DemoProfileResponse> DEMO_PROFILES = List.of(
            new DemoProfileResponse(
                    "CANDIDATE",
                    "Alex Morgan",
                    "demo.candidate@workplace.local",
                    "CandidateDemo123!",
                    "Browse roles, save opportunities, and review a sample application."
            ),
            new DemoProfileResponse(
                    "RECRUITER",
                    "Jordan Lee",
                    "demo.recruiter@workplace.local",
                    "RecruiterDemo123!",
                    "Explore a sample hiring dashboard, job listings, and candidate applications."
            )
    );

    private final boolean enabled;

    public DemoController(@Value("${app.demo.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @GetMapping("/profiles")
    public List<DemoProfileResponse> profiles() {
        return enabled ? DEMO_PROFILES : List.of();
    }
}
