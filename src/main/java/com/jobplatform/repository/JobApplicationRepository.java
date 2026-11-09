package com.jobplatform.repository;

import com.jobplatform.entity.Job;
import com.jobplatform.entity.JobApplication;
import com.jobplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByCandidateAndJob(User candidate, Job job);

    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<JobApplication> findByCandidate(User candidate, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company"})
    Page<JobApplication> findByJob(Job job, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company"})
    @Query(value = "select a from JobApplication a join a.job j where j.postedBy = :recruiter",
            countQuery = "select count(a) from JobApplication a join a.job j where j.postedBy = :recruiter")
    Page<JobApplication> findByRecruiter(@Param("recruiter") User recruiter, Pageable pageable);
}
