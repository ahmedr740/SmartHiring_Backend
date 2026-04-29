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

1. Create a PostgreSQL database named `smarthiring_db`
2. Update the username/password in `src/main/resources/application.properties`
3. From the project root, run:

```bash
./mvnw spring-boot:run
```

### Frontend

1. Go into the frontend folder:

```bash
cd staffmatch-frontend
```

2. Install dependencies and start the app:

```bash
npm install
npm start
```

The frontend runs on `http://localhost:3000` and the backend runs on `http://localhost:8080`.

## Notes About The Project

- This project was built in stages, so some parts are more polished than others.
- We focused more on getting the full flow working than on production-level architecture.
- Some extra demo/testing flows were kept in because they helped during integration and presentation prep.

## Possible Future Improvements

- stronger validation and error handling
- better automated test coverage
- email verification / password reset
- deployment configuration
- cleaner separation of DTOs and entities across the whole backend
