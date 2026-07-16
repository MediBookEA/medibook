# MediBook

A clinic appointment booking system where patients book 30-minute appointments with doctors.

## Quick Start

**Build:** `./mvnw -q compile`
**Run:** `./mvnw spring-boot:run` (starts on port 8080)
**Test:** `./mvnw test`

## API Endpoints

Base path: `/api/v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **Appointments** |
| POST | `/appointments` | Book a new appointment |
| DELETE | `/appointments/{id}` | Cancel an appointment |
| PUT | `/appointments/{id}` | Reschedule an appointment |
| GET | `/appointments?doctorId={id}&date={date}` | Get doctor's schedule by date |
| GET | `/appointments?doctorId={id}&from={date}` | Get doctor's upcoming appointments |
| **Patients** |
| POST | `/patients` | Register a new patient |
| GET | `/patients` | List all patients |
| GET | `/patients/{id}` | Retrieve a single patient |
| PUT | `/patients/{id}` | Update patient information |
| **Doctors** |
| GET | `/doctors` | List all available doctors |

## Documentation

See [`docs/api.md`](docs/api.md) for complete endpoint documentation with examples.

## Architecture

- **Backend:** Spring Boot 4 (Java 21), Maven
- **Database:** PostgreSQL (Docker), H2 (tests)
- **Frontend:** React + Vite (in `/frontend`)

Strict layering: Controller → Service → Repository. All DTOs are Java records in the `api.dto` package.

## Domain Rules

- Appointments are exactly 30 minutes (start on hour or half-hour)
- No overlapping booked appointments (per patient, per doctor)
- All appointments must be within the doctor's working hours
- All appointments must be in the future
- Cancellation sets status to CANCELLED (slots become rebookable)
- Rescheduling validates same rules as initial booking
