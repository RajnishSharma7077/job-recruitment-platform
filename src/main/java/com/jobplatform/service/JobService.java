package com.jobplatform.service;

import com.jobplatform.dto.JobRequest;
import com.jobplatform.dto.JobResponse;
import com.jobplatform.entity.Company;
import com.jobplatform.entity.Job;
import com.jobplatform.entity.User;
import com.jobplatform.repository.CompanyRepository;
import com.jobplatform.repository.JobRepository;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    public JobService(JobRepository jobRepository, CompanyRepository companyRepository) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
    }

    public Job createJob(JobRequest request, User recruiter) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        Job job = new Job(
                request.title(),
                request.description(),
                request.location(),
                request.employmentType(),
                request.minExperience(),
                request.salaryRange(),
                company,
                recruiter
        );

        return jobRepository.save(job);
    }

    public Page<Job> findAll(Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    public Page<Job> findByRecruiter(User recruiter, Pageable pageable) {
        return jobRepository.findByPostedBy(recruiter, pageable);
    }

    public Page<Job> findByFilters(String keyword, String location, Integer minExperience, Pageable pageable) {
        return jobRepository.search(normalize(keyword), normalize(location), minExperience, pageable);
    }

    public Job findById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getMinExperience(),
                job.getSalaryRange(),
                job.getCompany().getName(),
                job.getPostedBy().getName(),
                job.getStatus().name(),
                job.getCreatedAt()
        );
    }
}
