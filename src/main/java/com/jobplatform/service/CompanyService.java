package com.jobplatform.service;

import com.jobplatform.dto.CompanyRequest;
import com.jobplatform.dto.CompanyResponse;
import com.jobplatform.entity.Company;
import com.jobplatform.entity.Role;
import com.jobplatform.entity.User;
import com.jobplatform.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company createCompany(CompanyRequest request, User user) {
        if (user.getRole() != Role.RECRUITER && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Only recruiters and admins can create companies");
        }

        Company company = new Company(
                request.name(),
                request.description(),
                request.location(),
                request.website()
        );

        return companyRepository.save(company);
    }

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    }

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getLocation(),
                company.getWebsite()
        );
    }
}
