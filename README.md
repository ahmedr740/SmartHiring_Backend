# Smart Hiring

Smart Hiring is a group project for a college software/web development course. The idea behind the project is to help restaurant managers post short-term shifts and let workers apply for flexible jobs more easily.

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

3. Optional local/admin/settings:

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:3000
export ADMIN_SEED_ENABLED=true
export ADMIN_SEED_EMAIL=admin@example.com
export ADMIN_SEED_PASSWORD=replace-with-a-secure-password
export MATCHING_PROVIDER=ollama
export OLLAMA_MODEL=llama3.2:3b
export N8N_MATCHING_ENABLED=false
```

If Ollama is not running, the app still works and uses the built-in deterministic matching fallback.

### Database schema

Schema changes live in `src/main/resources/db/migration/` and are applied by Flyway on startup. Hibernate is set to `validate` by default — it checks that tables match the Java entities but does not alter the database. For deployed or shared environments, keep `JPA_DDL_AUTO=validate`. Only set `JPA_DDL_AUTO=update` temporarily on your own machine if you are actively experimenting with entity changes before writing a migration.

4. From the project root, run:

```bash
./mvnw spring-boot:run
```

### Frontend

1. Go into the frontend folder:

```bash
cd staffmatch-frontend
```

2. Optional: point the frontend at a different backend:

```bash
export REACT_APP_API_BASE_URL=http://localhost:8080/api
```

3. Install dependencies and start the app:

```bash
npm install
npm start
```

The frontend runs on `http://localhost:3000` and the backend runs on `http://localhost:8080`.

## Local AI Matching With Ollama (default)

The React app only talks to Spring. Spring calls Ollama on your machine to rank job matches — no paid API key required.

1. Install and start [Ollama](https://ollama.com), then pull a small local model:

```bash
ollama pull llama3.2:3b
```

2. Make sure Ollama is running on `http://localhost:11434` (default).

3. Start the backend with AI matching enabled (this is the default):

```bash
export MATCHING_PROVIDER=ollama
export OLLAMA_MODEL=llama3.2:3b
./mvnw spring-boot:run
```

4. Log in as a worker and open **AI Job Match** (`/worker-matches`), or log in as a manager and view applicant scores on the dashboard.

When AI works, match cards show `(AI)` and `source` is `OLLAMA`. If Ollama is off or slow, Spring falls back to built-in scoring and shows `(Fallback)`.

### Optional: n8n + Ollama instead of direct Ollama

If you prefer routing AI through n8n workflows, set `MATCHING_PROVIDER=n8n` and follow `docs/n8n/README.md`.

## Local AI Matching With n8n + Ollama (optional)

This project does not need a paid AI key. React only calls the Spring backend. With `MATCHING_PROVIDER=n8n`, Spring calls local n8n webhooks, and n8n can call Ollama on your own machine.

1. Install and start Ollama, then pull a small local model:

```bash
ollama pull llama3.2:3b
```

2. Run n8n locally on `http://localhost:5678`.

3. Create two n8n workflows:

- worker shift match webhook: `/webhook/staffmatch/worker-shift-match`
- manager applicant match webhook: `/webhook/staffmatch/manager-applicant-match`

Each workflow should start with a Webhook node, validate the `X-StaffMatch-Webhook-Secret` header, send the sanitized request body to Ollama with an HTTP Request node, normalize the result in a Code node, and return JSON with Respond to Webhook.

The response should include `targetId`, `aiScore`, `fallbackScore`, `label`, `explanation`, `strengths`, `risks`, `recommendedAction`, and `source`. Use `source: "N8N_OLLAMA"` for local AI responses. If the webhook is off, slow, or returns an error, Spring shows: "Local AI is unavailable, using built-in match score."

## Notes About The Project

- This project was built in stages, so some parts are more polished than others.
- We focused more on getting the full flow working than on production-level architecture.
- Some extra demo/testing flows were kept in because they helped during integration and presentation prep.
- Demo reset/bootstrap endpoints are no longer publicly usable. Use environment-controlled admin seeding for local setup.
- AI matching is server-side only. The frontend reads match recommendations from `/api/matches/...` and never talks directly to n8n or Ollama.

## Possible Future Improvements

- stronger validation and error handling
- better automated test coverage
- email verification / password reset
- deployment configuration
- cleaner separation of DTOs and entities across the whole backend
