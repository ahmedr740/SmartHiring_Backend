# HubPin

HubPin is a group project for a college software/web development course. The idea behind the project is to help restaurant managers post short-term shifts and let workers apply for flexible jobs more easily.

## AWS Hong Kong deployment

Production packaging for a temporary AWS Lightsail deployment is included in `docker-compose.prod.yml`. It runs the frontend, backend, PostgreSQL, n8n 2.26.8, daily backups, and Caddy HTTPS on one 4 GB instance. Follow [the complete Hong Kong deployment, DeepSeek, testing, backup, and teardown guide](docs/deployment/AWS_LIGHTSAIL_HONG_KONG.md).

All AI features use DeepSeek through protected n8n webhooks. Deterministic scoring remains available as `FALLBACK` when matching is unavailable.

## What The App Does

- workers can register, log in, and apply for shifts
- managers can register, post shifts, and manage applications
- admins can review manager accounts and monitor activity
- both sides can leave ratings after completed shifts

## Project Structure

- `src/main/java/...` contains the Spring Boot backend
- `src/main/resources/application.properties` contains backend config
- `staffmatch-frontend/` contains the React frontend

Note: the frontend folder still uses the older `staffmatch` name because we started with that project name early on and kept the folder path to avoid breaking local setup during development.

## Submission Scope

The initial project documents described a React Native mobile app. The current implementation target is now a React JS website, supported by the same Spring Boot and PostgreSQL backend. This website is the official current project direction for the submission, not a temporary replacement.

Essential functions currently covered:

- authentication and role-based access for workers, managers, and admins
- worker profile, job browsing, liked jobs, applications, and ratings
- manager shift posting, applicant review, shift lifecycle updates, mock payment checkout, and worker ratings
- admin manager approval, user moderation, shift/application inspection, and issue report tracking
- AI/fallback matching for workers and managers
- accepted-shift chat between managers and workers
- simple browser notifications while the website is open
- repeatable demo accounts and demo data for presentation

Near-future enhancements:

- real payment gateway
- full bilingual interface

## Tech Stack

- frontend: React, React Router, Axios, Tailwind CSS
- backend: Spring Boot, Spring Security, Spring Data JPA
- database: PostgreSQL

## Running The Project

### Backend

1. Create a PostgreSQL database named `staffmatch_db`
2. Copy `.env.example` to `.env` and set your local values (especially `DATABASE_PASSWORD` and `JWT_SECRET`). The `.env` file is gitignored — secrets must not go in `application.properties`.

```bash
cp .env.example .env
# edit .env, then load it:
set -a && source .env && set +a
```

Required variables:

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | DB user |
| `DATABASE_PASSWORD` | DB password |
| `JWT_SECRET` | Signing key for login tokens (32+ characters) |

Windows PowerShell example:

```powershell
Copy-Item .env.example .env
# Edit .env and set DATABASE_PASSWORD plus a 32+ character JWT_SECRET.
```

3. Optional local/admin/settings:

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:3000
export ADMIN_SEED_ENABLED=true
export ADMIN_SEED_EMAIL=admin@example.com
export ADMIN_SEED_PASSWORD=replace-with-a-secure-password
export MATCHING_ENABLED=true
export N8N_WORKER_MATCH_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/worker-shift-match
export N8N_MANAGER_MATCH_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/manager-applicant-match
export N8N_SHIFT_DRAFT_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/shift-draft
export N8N_SHIFT_SEARCH_WEBHOOK_URL=http://localhost:5678/webhook/staffmatch/shift-search
```

DeepSeek credentials are configured only in n8n. If a webhook is unavailable, matching uses the built-in deterministic fallback and the assistant screens retain their manual controls.

### Database schema

Schema changes live in `src/main/resources/db/migration/` and are applied by Flyway on startup. Hibernate is set to `validate` by default — it checks that tables match the Java entities but does not alter the database. For deployed or shared environments, keep `JPA_DDL_AUTO=validate`. Only set `JPA_DDL_AUTO=update` temporarily on your own machine if you are actively experimenting with entity changes before writing a migration.

4. From the project root, run:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend uses port `8080` by default. If that port is already busy, run it on `8081`:

```powershell
$env:SERVER_PORT='8081'
.\mvnw.cmd spring-boot:run
```

### Demo seed data

Demo data is off by default. For a presentation run, turn it on before starting the backend:

```powershell
$env:DEMO_SEED_ENABLED='true'
$env:DEMO_SEED_PASSWORD='SmartHiringDemo123!'
.\mvnw.cmd spring-boot:run
```

Seeded demo accounts:

| Role | Email | Password |
|------|-------|----------|
| Worker | `demo.worker@smarthiring.local` | `SmartHiringDemo123!` |
| Manager | `demo.manager@smarthiring.local` | `SmartHiringDemo123!` |
| Admin | `demo.admin@smarthiring.local` | `SmartHiringDemo123!` |

The seed also creates open shifts, pending applications, one completed unpaid shift for ratings/payment, one starter chat conversation, one mock payment record, and one pending manager account for admin review.

### Demo Script

1. Log in as admin and approve/suspend users, inspect shifts/applications, and review issue reports.
2. Log in as manager and enable browser notifications, create a shift, review applicants, accept a worker, open the accepted-shift chat, move the shift through the lifecycle, complete the mock payment checkout, and rate the worker.
3. Log in as worker and enable browser notifications, update the profile, browse open jobs, view AI job matches, like/apply to jobs, open accepted-shift chat, view history, submit a manager rating, and submit an issue report.
4. For browser notifications, keep the website tab open. The app uses the browser Notification API only; it does not use native/background push.
5. For payment, use the mock checkout. It is demo-only and does not process real money.

### Frontend

1. Go into the frontend folder:

```bash
cd staffmatch-frontend
```

2. Optional: point the frontend at a different backend:

```bash
export REACT_APP_API_BASE_URL=http://localhost:8080/api
```

On Windows PowerShell:

```powershell
$env:REACT_APP_API_BASE_URL='http://localhost:8080/api'
```

If the backend is running on `8081`, use:

```powershell
$env:REACT_APP_API_BASE_URL='http://localhost:8081/api'
```

3. Install dependencies and start the app:

```bash
npm install
npm start
```

On Windows PowerShell:

```powershell
npm.cmd install
npm.cmd start
```

The frontend runs on `http://localhost:3000` and the backend runs on `http://localhost:8080`.

### Demo readiness checks

Backend:

```powershell
.\mvnw.cmd test
```

Frontend:

```powershell
cd staffmatch-frontend
npm.cmd run build
$env:CI='true'
npm.cmd test -- --watchAll=false --watchman=false
```

## DeepSeek AI through n8n

React calls only the Spring backend. Spring sends task data to protected n8n webhooks, and n8n calls DeepSeek with its server-side credential. The DeepSeek API key is never stored in React or Spring.

Publish the four hosted DeepSeek workflows documented in [`docs/n8n/README.md`](docs/n8n/README.md):

- worker shift match webhook: `/webhook/staffmatch/worker-shift-match`
- manager applicant match webhook: `/webhook/staffmatch/manager-applicant-match`
- manager shift draft webhook: `/webhook/staffmatch/shift-draft`
- worker shift search webhook: `/webhook/staffmatch/shift-search`

Every valid AI response is recorded as `N8N_DEEPSEEK`. If DeepSeek or n8n is unavailable, Spring uses the built-in score.

## Notes About The Project

- This project was built in stages, so some parts are more polished than others.
- We focused more on getting the full flow working than on production-level architecture.
- Some extra demo/testing flows were kept in because they helped during integration and presentation prep.
- Demo reset/bootstrap endpoints are no longer publicly usable. Use environment-controlled admin seeding for local setup.
- AI matching is server-side only. The frontend reads match recommendations from `/api/matches/...` and never talks directly to n8n or DeepSeek.

## Possible Future Improvements

- stronger validation and error handling
- better automated test coverage
- email verification / password reset
- deployment configuration
- cleaner separation of DTOs and entities across the whole backend
