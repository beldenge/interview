# Java Spring Boot API Coding Exercise

This implementation models a self-service vehicle portal. Authenticated `VEHICLE_OWNER` users can manage only their own vehicles, while `ADMIN` users can manage all vehicle records.

## Steps to get started:

#### Prerequisites
- Maven
- Java 25

#### Fork the repository and clone it locally
- https://github.com/Tekmetric/interview.git

#### Import project into IDE
- Project root is located in `backend` folder

#### Build and run your app
- `mvn package && java -jar target/interview-1.0-SNAPSHOT.jar`
- `docker compose up --build`

#### Test that your app is running
- `curl -X GET   http://localhost:8080/api/welcome`
- `curl -X GET   http://localhost:8080/actuator/health`

#### After finishing the goals listed below create a PR

### Goals
1. Design a CRUD API with data store using Spring Boot and in memory H2 database (pre-configured, see below)
2. API should include one object with create, read, update, and delete operations. Read should include fetching a single item and list of items.
3. Provide SQL create scripts for your object(s)
4. Demo API functionality using API client tool

### Considerations
This is an open ended exercise for you to showcase what you know! We encourage you to think about best practices for structuring your code and handling different scenarios. Feel free to include additional improvements that you believe are important.

#### H2 Configuration
- Console: http://localhost:8080/h2-console 
- JDBC URL: jdbc:h2:mem:testdb
- Username: sa
- Password: password

#### Profiles
- Default profile is `h2`, which uses the in-memory database and local seed data.
- To run with PostgreSQL, start the database with `docker compose up -d postgres` and run the app with `--spring.profiles.active=postgres`.
- To run the full stack in containers, use `docker compose up --build`.
- Database schema and demo seed data are applied via Flyway migrations in `src/main/resources/db/migration`.
- Full API integration tests use Testcontainers PostgreSQL. When Docker is available, `mvn test` exercises the API against Postgres; otherwise that test class is skipped.
- PostgreSQL profile defaults:
  - JDBC URL: `jdbc:postgresql://localhost:5432/interview`
  - Username: `interview`
  - Password: `interview`

### Submitting your coding exercise
Once you have finished the coding exercise please create a PR into Tekmetric/interview

## Future Enhancements

If I continued evolving this project beyond the exercise, these are the next additions I would prioritize and how I would approach them:

- Migration maturity
  - The project now uses Flyway for schema creation and demo seed data. The next step would be to separate baseline schema from optional local/demo seed data and continue evolving the model through incremental versioned migrations.

- Production database parity
  - The application can already run against PostgreSQL, and the full API integration tests use Testcontainers PostgreSQL. The next step would be to expand that parity to more persistence-focused tests and CI so the main build always exercises the production-style database path.

- Better operational readiness
  - Expand Actuator usage with health, readiness, liveness, and build/info endpoints, then document which endpoints should be exposed in each environment. I would also disable development-only features such as the H2 console outside local use.

- Containerization
  - Add a `Dockerfile` and, if useful, a `docker-compose.yml` for local app plus database startup. That makes the project easier to run consistently locally.

- Environment configuration
  - Split configuration into local/test/prod profiles and push secrets or environment-specific values into environment variables. That keeps the app simple while showing a production-minded configuration model.

- API Versioning
  - If the API were to gain external consumers, I would introduce a clear versioning and deprecation strategy, most likely path-based versioning such as `/api/v1`. Alternatively, I would introduce a new GraphQL API and gradually migrate to it.

- Concurrency contract at the API boundary
  - The application already uses optimistic locking internally. A next step would be to expose that more explicitly to clients through a version field, then add integration tests that prove stale updates are rejected with `409 Conflict`.

- Rate limiting
  - If this moved beyond a coding exercise, I would introduce basic rate limiting at the edge.

- Observability
  - Add request correlation, structured logs, and metrics that surface key behaviors such as request counts, error rates, and latency. That would pair well with Actuator and make the service easier to operate and troubleshoot.

