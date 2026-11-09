package com.jobplatform.config;

import com.jobplatform.entity.Company;
import com.jobplatform.entity.JobApplication;
import com.jobplatform.entity.Job;
import com.jobplatform.entity.Role;
import com.jobplatform.entity.User;
import com.jobplatform.repository.CompanyRepository;
import com.jobplatform.repository.JobApplicationRepository;
import com.jobplatform.repository.JobRepository;
import com.jobplatform.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
                                     CompanyRepository companyRepository,
                                     JobRepository jobRepository,
                                     JobApplicationRepository applicationRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${app.admin.email:}") String adminEmail,
                                     @Value("${app.admin.password:}") String adminPassword,
                                     @Value("${app.seed-samples:false}") boolean seedSamples) {
        return args -> {
            if (adminEmail.isBlank() != adminPassword.isBlank()) {
                throw new IllegalStateException("Configure both JOBPLATFORM_ADMIN_EMAIL and JOBPLATFORM_ADMIN_PASSWORD");
            }

            if (!adminEmail.isBlank() && userRepository.findByEmail(adminEmail.trim().toLowerCase()).isEmpty()) {
                userRepository.save(new User(
                        "System Admin",
                        adminEmail.trim().toLowerCase(),
                        passwordEncoder.encode(adminPassword),
                        Role.ADMIN
                ));
            }

            if (!seedSamples) {
                return;
            }

            Company techHire = ensureCompany(companyRepository, "TechHire Solutions",
                    "Enterprise software and technology hiring", "Bengaluru", "https://techhire.example");
            Company greenLeaf = ensureCompany(companyRepository, "GreenLeaf Health",
                    "Digital health and patient-first care", "Hyderabad", "https://greenleaf.example");
            Company brightPath = ensureCompany(companyRepository, "BrightPath Learning",
                    "Learning tools for ambitious classrooms", "Pune", "https://brightpath.example");
            Company northstar = ensureCompany(companyRepository, "Northstar Finance",
                    "Simple, responsible financial services", "Mumbai", "https://northstar.example");
            Company loomLine = ensureCompany(companyRepository, "Loom & Line",
                    "Independent design and sustainable commerce", "Remote", "https://loomline.example");
            Company orbitCommerce = ensureCompany(companyRepository, "Orbit Commerce",
                    "Commerce infrastructure for growing brands", "Delhi NCR", "https://orbitcommerce.example");

            User demoRecruiter = ensureDemoUser(userRepository, passwordEncoder, "Jordan Lee",
                    "demo.recruiter@workplace.local", "RecruiterDemo123!", Role.RECRUITER);
            User demoCandidate = ensureDemoUser(userRepository, passwordEncoder, "Alex Morgan",
                    "demo.candidate@workplace.local", "CandidateDemo123!", Role.CANDIDATE);

            List<SampleJob> sampleJobs = List.of(
                    new SampleJob("Java Backend Engineer", "Build dependable APIs and services for products used by growing teams.", "Bengaluru", "Full-time", 2, "₹12–18 LPA", techHire),
                    new SampleJob("Frontend Developer", "Create accessible, polished interfaces with a product-minded engineering team.", "Remote", "Full-time", 1, "₹8–14 LPA", techHire),
                    new SampleJob("Data Analyst", "Turn customer and product data into clear insights for decision makers.", "Hyderabad", "Full-time", 1, "₹7–11 LPA", techHire),
                    new SampleJob("QA Automation Engineer", "Improve release confidence by building reliable automated test suites.", "Pune", "Full-time", 2, "₹9–14 LPA", techHire),
                    new SampleJob("Product Designer", "Research, prototype, and shape thoughtful experiences from discovery to delivery.", "Remote", "Full-time", 3, "₹14–22 LPA", loomLine),
                    new SampleJob("Visual Brand Designer", "Create a consistent visual language across campaigns, packaging, and digital products.", "Mumbai", "Contract", 2, "₹6–9 LPA", loomLine),
                    new SampleJob("Content Marketing Associate", "Write useful stories and campaigns that help more people discover our products.", "Remote", "Full-time", 0, "₹5–8 LPA", loomLine),
                    new SampleJob("Performance Marketing Manager", "Plan and optimize acquisition campaigns across paid channels.", "Delhi NCR", "Full-time", 4, "₹14–20 LPA", orbitCommerce),
                    new SampleJob("Customer Success Specialist", "Help merchants launch smoothly and get lasting value from the platform.", "Bengaluru", "Full-time", 1, "₹6–9 LPA", orbitCommerce),
                    new SampleJob("Operations Coordinator", "Keep partner onboarding, schedules, and day-to-day operations running well.", "Chennai", "Full-time", 1, "₹4–6 LPA", orbitCommerce),
                    new SampleJob("Finance Operations Analyst", "Support reconciliations, reporting, and accurate month-end close.", "Mumbai", "Full-time", 2, "₹8–12 LPA", northstar),
                    new SampleJob("Risk and Compliance Associate", "Help review processes and strengthen responsible financial operations.", "Pune", "Full-time", 1, "₹6–10 LPA", northstar),
                    new SampleJob("Financial Planning Intern", "Work with the finance team on analysis, research, and planning projects.", "Mumbai", "Internship", 0, "₹20–30k / month", northstar),
                    new SampleJob("Learning Experience Designer", "Design engaging learning journeys for students and educators.", "Remote", "Full-time", 2, "₹9–14 LPA", brightPath),
                    new SampleJob("Education Partnerships Executive", "Build relationships with schools and help educators adopt new tools.", "Jaipur", "Full-time", 1, "₹5–8 LPA", brightPath),
                    new SampleJob("Junior Curriculum Writer", "Create clear, inclusive learning materials for middle-school learners.", "Ahmedabad", "Part-time", 0, "₹3–5 LPA", brightPath),
                    new SampleJob("Customer Support Associate", "Give thoughtful help to customers over chat and email.", "Remote", "Part-time", 0, "₹3–5 LPA", greenLeaf),
                    new SampleJob("Healthcare Data Coordinator", "Maintain high-quality care data and support clinical operations teams.", "Hyderabad", "Full-time", 2, "₹7–11 LPA", greenLeaf),
                    new SampleJob("Community Health Program Manager", "Coordinate community programs with local partners and care teams.", "Kochi", "Contract", 3, "₹8–13 LPA", greenLeaf)
            );

            for (SampleJob sample : sampleJobs) {
                if (!jobRepository.existsByTitleAndCompany(sample.title(), sample.company())) {
                    Job job = new Job(
                            sample.title(),
                            sample.description(),
                            sample.location(),
                            sample.employmentType(),
                            sample.minExperience(),
                            sample.salaryRange(),
                            sample.company(),
                            demoRecruiter
                    );
                    jobRepository.save(job);
                }
            }

            Job demoJob = jobRepository.findByTitleAndCompany("Frontend Developer", techHire)
                    .orElseThrow(() -> new IllegalStateException("Demo job was not seeded"));
            if (!applicationRepository.existsByCandidateAndJob(demoCandidate, demoJob)) {
                applicationRepository.save(new JobApplication(
                        demoJob,
                        demoCandidate,
                        "https://example.com/alex-morgan-resume.pdf"
                ));
            }
        };
    }

    private User ensureDemoUser(UserRepository repository, PasswordEncoder passwordEncoder,
                                String name, String email, String password, Role role) {
        return repository.findByEmail(email)
                .orElseGet(() -> repository.save(new User(
                        name, email, passwordEncoder.encode(password), role
                )));
    }

    private Company ensureCompany(CompanyRepository repository, String name, String description,
                                  String location, String website) {
        return repository.findByName(name)
                .orElseGet(() -> repository.save(new Company(name, description, location, website)));
    }

    private record SampleJob(String title, String description, String location, String employmentType,
                             Integer minExperience, String salaryRange, Company company) {
    }
}
