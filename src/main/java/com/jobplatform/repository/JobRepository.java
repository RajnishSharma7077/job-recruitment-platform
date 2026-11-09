package com.jobplatform.repository;

import com.jobplatform.entity.Job;
import com.jobplatform.entity.Company;
import com.jobplatform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    @EntityGraph(attributePaths = {"company", "postedBy"})
    Page<Job> findByPostedBy(User postedBy, Pageable pageable);

    boolean existsByTitleAndCompany(String title, Company company);

    Optional<Job> findByTitleAndCompany(String title, Company company);

    @Override
    @EntityGraph(attributePaths = {"company", "postedBy"})
    Page<Job> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"company", "postedBy"})
    @Query("""
            select j from Job j
            where (:keyword is null
                or lower(j.title) like lower(concat('%', :keyword, '%'))
                or lower(j.description) like lower(concat('%', :keyword, '%')))
              and (:location is null
                or lower(j.location) like lower(concat('%', :location, '%')))
              and (:minExperience is null
                or j.minExperience is null
                or j.minExperience <= :minExperience)
            """)
    Page<Job> search(@Param("keyword") String keyword,
                     @Param("location") String location,
                     @Param("minExperience") Integer minExperience,
                     Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"company", "postedBy"})
    Optional<Job> findById(Long id);
}
