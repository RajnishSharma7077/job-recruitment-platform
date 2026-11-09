# Job Recruitment Platform

A Spring Boot-based recruitment platform with role-based access for candidates, recruiters, and admins.

## Features
- Separate home, job search, and employer pages
- Job search by keyword and location, with remote, early-career, and employment-type filters
- Database-backed job search with bounded pages, plus indexed, sortable salary, experience, and posting-date fields
- Rich job cards, sorting by posting date, salary, or experience, and dedicated job-detail pages
- 19 sample openings across technology, design, commerce, finance, education, and healthcare
- Sample roles span remote and in-person locations, internships, part-time, contract, and full-time work
- Save jobs in the current browser and save a search preference locally
- Candidate applications and recruiter dashboards
- User registration and login
- Stateless JWT authentication
- Candidate, recruiter, and admin roles
- Job posting and listing APIs
- Candidate job applications
- H2 in-memory database for local development; PostgreSQL profile for persistent deployments
- Flyway-managed schema migrations, Actuator health checks, and Prometheus metrics
- Spring Security with role-aware APIs

## Tech stack
- Java 17
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Security
- H2 Database

## Run locally
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
export JWT_SECRET="$(openssl rand -base64 48)"
mvn spring-boot:run
```

Open the website at [http://localhost:8080](http://localhost:8080). Dedicated pages are available at `/jobs.html`, `/job.html?id=<jobId>`, and `/employers.html`. Browse and search jobs without signing in; create a candidate or recruiter account to apply or post roles.

The default `local` profile binds to `127.0.0.1`, uses H2, and seeds sample companies, roles, and candidate/recruiter demo profiles at startup. The sign-in dialog offers **How Workplace works** and one-click demo previews for both roles. Sample demo accounts and that preview endpoint are enabled only in the local profile; production disables them. The in-memory database resets when the app stops; it is for development only. Flyway applies the versioned schema migrations in each profile.

### Production database profile

The `prod` profile uses PostgreSQL, disables the H2 console and sample listings, and requires database credentials. It also installs PostgreSQL trigram indexes for substring job search; the database role used at migration time must be allowed to create the `pg_trgm` extension (or have it pre-enabled). Configure `DATABASE_URL` (a JDBC URL), `DB_USERNAME`, `DB_PASSWORD`, and a strong `JWT_SECRET` of at least 32 bytes before starting:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://db-host:5432/jobplatform
export DB_USERNAME=jobplatform
export DB_PASSWORD='use-a-managed-secret'
export JWT_SECRET="$(openssl rand -base64 48)"
export JOBPLATFORM_ADMIN_EMAIL=admin@example.com
export JOBPLATFORM_ADMIN_PASSWORD='use-a-different-strong-password'
mvn spring-boot:run
```

Use a managed PostgreSQL service or a separately operated PostgreSQL server for deployment. Persist and regularly back up its data, test restoration, and keep credentials in the hosting provider's secret manager rather than in source control. The app applies Flyway migrations at startup; production Hibernate schema handling is validation-only. Sample/demo jobs are never seeded in the `prod` profile. The optional admin bootstrap creates an account only when both admin environment variables are set.

For a single-host container deployment, copy `.env.example` to `.env`, replace every placeholder, and run:

```bash
cp .env.example .env
openssl rand -base64 48
docker compose up --build -d
docker compose ps
```

The Compose setup keeps PostgreSQL data in a named volume, waits for the database health check before starting the app, restarts failed containers, and binds the app to localhost so a TLS-enabled reverse proxy can sit in front. It is a starting point for one host, not a substitute for managed database backups, monitoring, firewalling, TLS, or a tested disaster-recovery plan. Back up PostgreSQL with `pg_dump` to storage outside the host and periodically test a restore.

### Capacity and operations

- Job searches, recruiter job lists, and application lists are database-paginated (20 rows by default, maximum 100). The jobs page fetches additional pages on demand.
- HikariCP and Tomcat use bounded conservative pools/backlogs by default. Set `DB_POOL_SIZE`, `DB_POOL_MIN_IDLE`, `HTTP_MAX_THREADS`, `HTTP_MIN_SPARE_THREADS`, `HTTP_MAX_CONNECTIONS`, and `HTTP_ACCEPT_COUNT` only after measuring the database and total number of app instances; the sum of all instance database pools must stay below the database connection limit.
- `GET /actuator/health` and `GET /actuator/health/readiness` provide health/readiness checks. `/actuator/prometheus` exports metrics and requires authentication; scrape it only from a trusted monitoring network.
- Graceful shutdown and HTTP response compression are enabled. A production host should restart crashed processes, retain logs, configure TLS and request-rate controls at its edge, and alert on error rate, latency, JVM memory, and database-pool saturation.
- Before increasing instance or thread counts, run a load test against a production-like PostgreSQL dataset and hosting environment. This repository does not claim a throughput limit; the safe capacity depends on database size, query mix, and deployment resources.

To bootstrap an admin, set both environment variables before starting the app:
```bash
export JOBPLATFORM_ADMIN_EMAIL=admin@example.com
export JOBPLATFORM_ADMIN_PASSWORD='use-a-strong-password'
```
The application no longer creates a default admin account. Never use development credentials in a deployed environment.

Authentication endpoints return a bearer token in the `token` field. Send it on protected requests:
```http
Authorization: Bearer <token>
```

## Endpoints
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/companies`
- `POST /api/companies`
- `GET /api/jobs`
- `GET /api/jobs/{jobId}`
- `GET /api/jobs/search?keyword=&location=&minExperience=&page=0&size=20&sort=newest`
- `GET /api/jobs/my`
- `POST /api/jobs`
- `GET /api/jobs/{jobId}/applications`
- `POST /api/applications/apply`
- `GET /api/applications/mine`
- `GET /api/applications/recruiter/me`

The browser app is served by Spring Boot from `src/main/resources/static`, so it uses the same origin and does not need a separate frontend server. Saved jobs and saved search preferences are currently stored in the browser only; email alerts and the employer workflow ideas shown as “Next up” are not implemented yet. Paginated API responses contain `content`, `page`, `size`, `totalElements`, `totalPages`, and `last`.

## H2 Console
Open:
```text
http://localhost:8080/h2-console
```
Use JDBC URL:
```text
jdbc:h2:mem:jobrecruitment
```
Username: `sa`
Password: empty
