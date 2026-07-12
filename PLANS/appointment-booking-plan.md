# Plan: Appointment Booking Feature

## Context
MediBook is a greenfield project — no Java source exists yet. This plan covers the
`AppointmentService` (with all validation logic), the supporting entities/DTOs/repositories
it depends on, and the `AppointmentController` REST endpoint. Rescheduling and cancellation
are included because their validation path is shared with booking and must be planned together.

---

## Edge Cases in the Double-Booking Rule

These drove several design decisions below.

1. **Back-to-back is NOT an overlap.** 09:00–09:30 and 09:30–10:00 touch but must both
   be ALLOWED. The overlap predicate must be strict: `existingStart < newEnd AND existingEnd > newStart`
   — no `<=` or `>=`.

2. **Cancelled slots must not block.** The overlap query filters `status = BOOKED` only.
   A CANCELLED slot at 09:00 must allow a new BOOKED slot at 09:00.

3. **Rescheduling self-conflict.** When rescheduling appointment X, the overlap query must
   exclude X itself. Otherwise a new slot that overlaps the *current* slot would be wrongly
   rejected. Solved by an `excludeId` parameter: `null` for new bookings, the appointment's
   own ID for rescheduling.

4. **Working-hours end boundary includes the last slot.** A doctor finishing at 17:00 must
   allow the 16:30–17:00 slot (`endTime <= workingHoursEnd`, not `<`). Starting at 17:00
   is invalid (would end at 17:30).

5. **Doctor not working that day.** If no `WorkingHours` row exists for the appointment's
   `DayOfWeek`, treat it the same as outside working hours → 422.

6. **Storing `endTime` on the entity.** JPQL cannot do date arithmetic on columns, and
   native SQL with `DATEADD` diverges between H2 (tests) and PostgreSQL (dev). Storing
   `endTime` as a persisted column (set once at creation to `startTime + 30 min`, never
   mutated) keeps the overlap query pure JPQL and database-portable.

---

## Entities (domain layer)

**`AppointmentStatus`** — enum: `BOOKED`, `CANCELLED`

**`Patient`** — `id` (Long, PK), `name`, `email` (unique), `dateOfBirth` (LocalDate), `phone`

**`Doctor`** — `id`, `name`, `specialty`, `workingHours` (`@OneToMany(mappedBy="doctor")`)

**`WorkingHours`** — `id`, `doctor` (`@ManyToOne`), `dayOfWeek` (`DayOfWeek`, `@Enumerated(STRING)`),
`startTime` (LocalTime), `endTime` (LocalTime)

**`Appointment`** — `id`, `patient` (`@ManyToOne`), `doctor` (`@ManyToOne`),
`startTime` (LocalDateTime), `endTime` (LocalDateTime, persisted), `status` (`@Enumerated(STRING)`)

---

## DTOs (`com.medibook.api.dto`, Java records)

```
BookAppointmentRequest(Long patientId, Long doctorId, LocalDateTime startTime)
    — @NotNull on all three fields

RescheduleAppointmentRequest(LocalDateTime startTime)
    — @NotNull on startTime

AppointmentResponse(Long id,
                    Long patientId, String patientName,
                    Long doctorId,  String doctorName,
                    LocalDateTime startTime, LocalDateTime endTime,
                    String status)

ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path)
```

---

## Custom Exceptions (`com.medibook.exception`)

| Exception | HTTP | `error` field |
|---|---|---|
| `PatientNotFoundException` | 404 | `NOT_FOUND` |
| `DoctorNotFoundException` | 404 | `NOT_FOUND` |
| `AppointmentNotFoundException` | 404 | `NOT_FOUND` |
| `DoubleBookingException` | 409 | `DOUBLE_BOOKING` |
| `OutsideWorkingHoursException` | 422 | `OUTSIDE_WORKING_HOURS` |
| `AppointmentInPastException` | 422 | `APPOINTMENT_IN_PAST` |
| `InvalidStartTimeException` | 422 | `INVALID_START_TIME` |

All are simple `RuntimeException` subclasses carrying a human-readable message.

---

## AppointmentRepository (custom JPQL)

Two symmetric queries — one for doctor, one for patient. The doctor variant:

```jpql
@Query("""
    SELECT a FROM Appointment a
    WHERE a.doctor.id  = :doctorId
      AND a.status     = com.medibook.domain.AppointmentStatus.BOOKED
      AND a.startTime  < :endTime
      AND a.endTime    > :startTime
      AND (:excludeId IS NULL OR a.id <> :excludeId)
    """)
List<Appointment> findOverlappingForDoctor(Long doctorId,
    LocalDateTime startTime, LocalDateTime endTime, Long excludeId);
```

Patient variant: same structure with `a.patient.id = :patientId`.

The `(:excludeId IS NULL OR ...)` idiom handles both booking (pass `null`) and
rescheduling (pass own ID) without a separate method.

---

## AppointmentService

**Constructor:** `AppointmentService(AppointmentRepository, PatientRepository, DoctorRepository)`

### Private helper: `validateSlot(startTime, doctorId, patientId, excludeId)`

Shared by `bookAppointment` (excludeId = null) and `rescheduleAppointment` (excludeId = appointmentId).

Ordered validation steps:
1. `startTime.isAfter(LocalDateTime.now())` — else throw `AppointmentInPastException` (422)
2. `startTime.getMinute() ∈ {0, 30}` AND `startTime.getSecond() == 0` — else `InvalidStartTimeException` (422)
3. `patientRepository.findById(patientId)` — else `PatientNotFoundException` (404)
4. `doctorRepository.findById(doctorId)` — else `DoctorNotFoundException` (404)
5. Compute `endTime = startTime.plusMinutes(30)`
6. Find `WorkingHours` for `startTime.getDayOfWeek()` in `doctor.getWorkingHours()`; if none → `OutsideWorkingHoursException` (422). Else verify `startTime.toLocalTime() >= wh.startTime()` AND `endTime.toLocalTime() <= wh.endTime()` — else same exception.
7. `findOverlappingForDoctor(doctorId, startTime, endTime, excludeId)` non-empty → `DoubleBookingException` (409)
8. `findOverlappingForPatient(patientId, startTime, endTime, excludeId)` non-empty → `DoubleBookingException` (409)

### Public methods

**`bookAppointment(BookAppointmentRequest)`** → `AppointmentResponse`
- Calls `validateSlot(..., null)`
- Builds `Appointment` with `status = BOOKED`, `endTime = startTime.plusMinutes(30)`, saves, maps to response.

**`cancelAppointment(Long id)`** → `AppointmentResponse`
- Fetches appointment → 404 if absent
- Sets `status = CANCELLED`, saves, maps to response.

**`rescheduleAppointment(Long id, RescheduleAppointmentRequest)`** → `AppointmentResponse`
- Fetches appointment → 404 if absent
- Calls `validateSlot(newStartTime, doctorId, patientId, appointmentId)`
- Updates `startTime` and `endTime`, saves, maps to response.

---

## AppointmentController

`@RestController @RequestMapping("/api/v1/appointments")`
Constructor: `AppointmentController(AppointmentService)`

| Verb | Path | Body | Returns |
|---|---|---|---|
| `POST` | `/api/v1/appointments` | `@Valid BookAppointmentRequest` | 201 + `AppointmentResponse` |
| `DELETE` | `/api/v1/appointments/{id}` | — | 200 + `AppointmentResponse` |
| `PUT` | `/api/v1/appointments/{id}` | `@Valid RescheduleAppointmentRequest` | 200 + `AppointmentResponse` |
| `GET` | `/api/v1/appointments/{id}` | — | 200 + `AppointmentResponse` |

Zero business logic in the controller. All exceptions bubble to `GlobalExceptionHandler`.

---

## GlobalExceptionHandler (`com.medibook.config`, `@RestControllerAdvice`)

One `@ExceptionHandler` method per exception type above, plus one for
`MethodArgumentNotValidException` → 400 / `VALIDATION_ERROR`.

Each handler receives `HttpServletRequest` to fill `ErrorResponse.path`, and uses
`LocalDateTime.now()` for `timestamp`.

---

## TDD Test Plan

### AppointmentServiceTest (JUnit 5 + Mockito, no Spring context)

**Happy path**
- `book_happyPath_returnsSavedAppointmentResponse`
- `cancel_happyPath_setsStatusToCancelled`
- `reschedule_happyPath_returnsUpdatedAppointmentResponse`

**Future check**
- `book_rejectsPastStartTime`
- `book_rejectsStartTimeAtExactlyNow`

**Alignment check**
- `book_rejectsMinuteNotOnHalfHour` (e.g. 09:15)
- `book_rejectsNonZeroSeconds`
- `book_acceptsStartOnHour`
- `book_acceptsStartOnHalfHour`

**404s**
- `book_rejectsUnknownPatientId`
- `book_rejectsUnknownDoctorId`
- `cancel_rejectsUnknownId`
- `reschedule_rejectsUnknownId`

**Working hours**
- `book_rejectsStartBeforeWorkingHoursStart`
- `book_rejectsEndAfterWorkingHoursEnd`
- `book_rejectsStartExactlyAtWorkingHoursEnd` (e.g. 17:00 for a 17:00 close)
- `book_acceptsLastValidSlot` (16:30 for a 17:00 close)
- `book_acceptsStartExactlyAtWorkingHoursStart`
- `book_rejectsDayDoctorDoesNotWork`

**Doctor double-booking**
- `book_rejectsDoctorConflict_exactSameSlot`
- `book_rejectsDoctorConflict_partialOverlapLeading`
- `book_rejectsDoctorConflict_partialOverlapTrailing`
- `book_allowsDoctorBackToBackAppointments`

**Patient double-booking**
- `book_rejectsPatientConflict_exactSameSlot`
- `book_rejectsPatientConflict_partialOverlap`
- `book_allowsPatientBackToBackAppointments`

**Cancelled slots**
- `book_allowsRebookingCancelledDoctorSlot`
- `book_allowsRebookingCancelledPatientSlot`

**Rescheduling self-conflict**
- `reschedule_doesNotSelfConflict_onOwnCurrentSlot`
- `reschedule_rejectsConflictWithDifferentAppointment`

### AppointmentControllerTest (`@WebMvcTest`, `@MockBean AppointmentService`)

- `postAppointment_validRequest_returns201AndBody`
- `postAppointment_missingPatientId_returns400`
- `postAppointment_missingDoctorId_returns400`
- `postAppointment_missingStartTime_returns400`
- `postAppointment_serviceThrowsDoubleBooking_returns409`
- `postAppointment_serviceThrowsOutsideWorkingHours_returns422`
- `postAppointment_serviceThrowsInPast_returns422`
- `postAppointment_serviceThrowsPatientNotFound_returns404`
- `postAppointment_serviceThrowsDoctorNotFound_returns404`
- `postAppointment_errorBodyContainsAllRequiredFields` (timestamp, status, error, message, path)
- `deleteAppointment_existingId_returns200WithCancelledStatus`
- `deleteAppointment_unknownId_returns404`
- `putAppointment_validRequest_returns200`
- `putAppointment_missingStartTime_returns400`
- `putAppointment_serviceThrowsDoubleBooking_returns409`

### AppointmentRepositoryTest (`@DataJpaTest`, H2)

- `findOverlappingForDoctor_returnsOverlappingBooked`
- `findOverlappingForDoctor_excludesCancelledAppointments`
- `findOverlappingForDoctor_returnsEmptyForBackToBackAppointments`
- `findOverlappingForDoctor_excludesOwnIdWhenExcludeIdProvided`
- `findOverlappingForPatient_returnsOverlappingBooked`
- `findOverlappingForPatient_returnsEmptyForBackToBackAppointments`

---

## Implementation Order (TDD-first within each step)

1. Enums + domain entities (no test needed — pure JPA annotations)
2. Exception classes
3. DTOs
4. `AppointmentRepository` queries → `AppointmentRepositoryTest` (test red → impl → green)
5. `AppointmentService` → `AppointmentServiceTest` (all tests red → impl → all green)
6. `GlobalExceptionHandler`
7. `AppointmentController` → `AppointmentControllerTest` (all tests red → impl → all green)

Each step is a separate commit. Run `./mvnw test` before every commit.

---

## Verification

1. `./mvnw test -Dtest=AppointmentServiceTest` — green, no Spring context
2. `./mvnw test -Dtest=AppointmentControllerTest` — green, status codes and error body shape verified
3. `./mvnw test -Dtest=AppointmentRepositoryTest` — JPQL overlap predicate verified on H2
4. `./mvnw test` — full build green
5. Manual smoke (after `./mvnw spring-boot:run`):
   - `POST /api/v1/appointments` valid slot → 201
   - Same slot again → 409 `DOUBLE_BOOKING`
   - Past slot → 422 `APPOINTMENT_IN_PAST`
   - Misaligned minute (09:15) → 422 `INVALID_START_TIME`
   - Unknown doctor ID → 404
   - `DELETE /{id}` → 200 CANCELLED; re-POST same slot → 201 (cancelled doesn't block)
   - `PUT /{id}` to own current slot → 200 (no self-conflict)
6. `./mvnw test jacoco:report` — service + repository branch coverage should be high given the test list
