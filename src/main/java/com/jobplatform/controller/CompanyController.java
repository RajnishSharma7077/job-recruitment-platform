package com.jobplatform.controller;

import com.jobplatform.dto.CompanyRequest;
import com.jobplatform.dto.CompanyResponse;
import com.jobplatform.entity.User;
import com.jobplatform.service.CompanyService;
import com.jobplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final UserService userService;

    public CompanyController(CompanyService companyService, UserService userService) {
        this.companyService = companyService;
        this.userService = userService;
    }

    @GetMapping
    public List<CompanyResponse> getAllCompanies() {
        return companyService.findAll().stream()
                .map(companyService::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyRequest request,
                                                        Principal principal) {
        User user = userService.findByEmail(principal.getName());
        return ResponseEntity.ok(companyService.toResponse(companyService.createCompany(request, user)));
    }
}
