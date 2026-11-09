package com.jobplatform.service;

import com.jobplatform.dto.ApplicationResponse;
import com.jobplatform.entity.Job;
import com.jobplatform.entity.JobApplication;
import com.jobplatform.entity.Role;
import com.jobplatform.entity.User;
import com.jobplatform.repository.JobApplicationRepository;
import com.jobplatform.repository.JobRepository;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public ApplicationService(JobApplicationRepository applicationRepository, JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    public JobApplication applyToJob(Long jobId, User candidate, String resumeUrl) {
        if (candidate.getRole() != Role.CANDIDATE) {
            throw new IllegalArgumentException("Only candidates can apply for jobs");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        JobApplication application = new JobApplication(job, candidate, resumeUrl);
        return applicationRepository.save(application);
    }

    public Page<JobApplication> findByCandidate(User candidate, Pageable pageable) {
        return applicationRepository.findByCandidate(candidate, pageable);
    }

    public Page<JobApplication> findByJob(Job job, int page, int size) {
        return applicationRepository.findByJob(job, applicationPage(page, size));
    }

    public Page<JobApplication> findByRecruiter(User recruiter, Pageable pageable) {
        return applicationRepository.findByRecruiter(recruiter, pageable);
    }

    public Pageable applicationPage(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.desc("id")));
    }

    public ApplicationResponse toResponse(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getJob().getCompany().getName(),
                application.getResumeUrl(),
                application.getStatus().name(),
                application.getAppliedAt()
        );
    }
}
