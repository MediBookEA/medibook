# CLAUDE.md — MediBook

## Project overview

MediBook is a clinic appointment booking system.patients book 30-minute appointments withdoctors

- Backend: Spring Boot 4, Java 26, Maven (single module, root of repo)
- Frontend: React + Vite, plain JavaScript, in `/frontend`
- Database: PostgreSQL via Docker for dev (`docker compose up -d`), H2 in-memory for tests

## Commands

- Build backend: `./mvnw -q compile`
- Run all tests: `./mvnw test`
- Run one test class: `./mvnw test -Dtest=AppointmentServiceTest`
- Coverage report: `./mvnw test jacoco:report` (output: `target/site/jacoco/index.html`)
- Run backend: `./mvnw spring-boot:run` (port 8080)
- Frontend dev server: `cd frontend && npm run dev` (port 5173, proxies /api to 8080)
- Frontend tests: `cd frontend && npm test`

## Architecture rules

- Strict layering: `controller` → `service` → `repository`. Never skip a layer.
- ALL business rules live in the service layer. Controllers only validate input,
  map DTOs, and translate exceptions. Repositories only do data access.
- Never expose JPA entities in the API. Every endpoint uses DTOs, written as Java
  records in the `api.dto` package.
- Package by layer: `com.medibook.controller`, `.service`, `.repository`,
  `.domain`, `.api.dto`, `.config`.
- Constructor injection only. No `@Autowired` on fields.
- Use `java.time.LocalDateTime` for appointment times. The clinic has ONE time
  zone; never introduce time zone handling.

## Domain rules (enforce these in AppointmentService)

- Appointments are exactly 30 minutes and start on the hour or half hour.
- A doctor cannot have two overlapping BOOKED appointments → 409 Conflict.
- A patient cannot have two overlapping BOOKED appointments → 409 Conflict.
- Appointments must fall inside the doctor's working hours → 422.
- Appointments must be in the future → 422.
- Cancelling sets status to CANCELLED (never hard-delete). Cancelled slots are
  bookable again.
- Rescheduling applies the exact same validation as booking.

## API conventions

- Base path `/api/v1`. Plural resource names: `/patients`, `/doctors`, `/appointments`.
- Status codes: 201 on create, 200 on read/update, 409 for booking conflicts,
  422 for business-rule violations, 400 for malformed input, 404 for unknown IDs.
- Every error response uses this exact shape (produced by GlobalExceptionHandler):

```json
{
  "timestamp": "2026-07-13T10:15:30",
  "status": 409,
  "error": "DOUBLE_BOOKING",
  "message": "Dr. Smith already has an appointment at 2026-07-14T09:00",
  "path": "/api/v1/appointments"
}
```

## Testing — follow TDD, always

- Write the failing test FIRST, show it failing, then implement, then run tests
  again. Never write implementation before its test exists.
- Service tests: plain JUnit 5 + Mockito, no Spring context. One test class per
  service. Cover every domain rule above, including edge cases (exact boundary
  of working hours, back-to-back appointments touching but not overlapping).
- Controller tests: `@WebMvcTest` slices with mocked services. Assert status
  codes AND the error body shape.
- Repository queries with custom logic: `@DataJpaTest` on H2.
- Test names describe behavior: `book_rejectsOverlappingAppointmentForSameDoctor`.
- After ANY code change, run `./mvnw test` before considering the task done.
- Do not weaken, delete, or @Disable a failing test to make the build pass.
  If a test seems wrong, stop and ask.

## Git workflow

- Never commit directly to `main`. Feature branches: `feature/<short-name>`.
- Conventional commits: `feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`.
- Small commits — one logical change each. Tests and their implementation go in
  the same commit.
- Before committing: run `./mvnw test`. Never commit with failing tests.
- Do not `git push --force` and do not amend commits that are already pushed.

## Scope guardrails

Do NOT add, even if it seems helpful: authentication, roles, email/notifications,
payments, time zones, recurring appointments, doctor admin CRUD, pagination,
caching, or new dependencies. If a task appears to need any of these, stop and
ask first. Doctors are seed data only (`src/main/resources/data.sql`).

## Frontend conventions

- Five views only: BookAppointment, DoctorDaySchedule, UpcomingSchedule
  (a doctor's upcoming appointments grouped by day), ManagePatients, and
  AllPatients (read-only patient directory).
  React function components with hooks, no class components, no state library,
  no router libraries beyond what already exists.
- All API calls go through `frontend/src/api.js`. Handle the standard error
  body and show its `message` to the user.
- Styling uses Tailwind CSS (utility classes in JSX), configured via
  `@tailwindcss/vite`. Keep it clean and modern; no component library beyond
  Tailwind itself.