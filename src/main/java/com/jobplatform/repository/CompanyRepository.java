package com.jobplatform.repository;

import com.jobplatform.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    java.util.Optional<Company> findByName(String name);
}
