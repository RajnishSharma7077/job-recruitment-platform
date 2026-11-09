package com.jobplatform.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String employmentType;

    private Integer minExperience;

    private String salaryRange;

    @Column(name = "salary_sort_value")
    private Double salarySortValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posted_by", nullable = false)
    private User postedBy;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobApplication> applications = new ArrayList<>();

    public Job() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Job(String title, String description, String location, String employmentType, Integer minExperience,
               String salaryRange, Company company, User postedBy) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.employmentType = employmentType;
        this.minExperience = minExperience;
        this.salaryRange = salaryRange;
        this.salarySortValue = normalizeSalary(salaryRange);
        this.company = company;
        this.postedBy = postedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Integer getMinExperience() {
        return minExperience;
    }

    public void setMinExperience(Integer minExperience) {
        this.minExperience = minExperience;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
        this.salarySortValue = normalizeSalary(salaryRange);
    }

    public Double getSalarySortValue() {
        return salarySortValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public User getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(User postedBy) {
        this.postedBy = postedBy;
    }

    public List<JobApplication> getApplications() {
        return applications;
    }

    public void setApplications(List<JobApplication> applications) {
        this.applications = applications;
    }

    private static Double normalizeSalary(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replace(",", "").toLowerCase();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)?").matcher(normalized);
        double total = 0;
        int count = 0;
        while (matcher.find()) {
            total += Double.parseDouble(matcher.group());
            count++;
        }
        if (count == 0) return null;

        double midpoint = total / count;
        if (normalized.contains("lpa") || normalized.contains("lakh")) return midpoint * 100_000;
        if (normalized.contains("month")) {
            return midpoint * (normalized.contains("k") ? 1_000 : 1) * 12;
        }
        if (normalized.contains("k")) return midpoint * 1_000;
        return null;
    }
}
