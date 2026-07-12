# Patient Registry Feature

## Context

MediBook currently has an `Appointment` feature end-to-end (domain, repository, service,
controller, exceptions, tests) but the `Patient` side only has the entity, the repository, and
a `PatientNotFoundException` — there is no way to register, list, retrieve, or update a patient
through the API yet. `data.sql` seeds only doctors; per the README, patients are meant to be
created purely through the API.

The README's functional requirement is explicit:

> "The system shall register a patient with name, email (unique), date of birth, and phone; an
> invalid or duplicate email is rejected with a 400/409 and a structured error body."
> "The system shall list and retrieve patients and doctors..."
> "...Update contact info"

This plan adds `PatientService` + `PatientController` + supporting DTOs/exception, mirroring the
existing `AppointmentService`/`AppointmentController` conventions exactly, and closes several
correctness gaps that a naive implementation would miss (case-sensitive email collisions, a
save-time race condition on the uniqueness check, and a newborn-DOB validation bug).

**Confirmed decisions** (via user sign-off):
1. Update is a **full replace** — `name`, `email`, `phone` all required in `UpdatePatientRequest` (no PATCH-style partial update), matching `RescheduleAppointmentRequest`'s all-required precedent.
2. **Date of birth is immutable** — excluded entirely from the update DTO; `Patient` keeps no `setDateOfBirth`.
3. **Email is normalized to lowercase** (trim + lowercase) before both the uniqueness check and storage/response, so "John@X.com" and "john@x.com" collide as the same patient.
4. **No phone format validation** beyond `@NotBlank` — only a value is required, not a specific format.

## Files to add

| File | Responsibility |
|---|---|
| `src/main/java/com/medibook/api/dto/RegisterPatientRequest.java` | `POST` body: `name`, `email`, `dateOfBirth`, `phone`, all validated |
| `src/main/java/com/medibook/api/dto/UpdatePatientRequest.java` | `PUT` body: `name`, `email`, `phone` (no DOB) |
| `src/main/java/com/medibook/api/dto/PatientResponse.java` | Output record: `id`, `name`, `email`, `dateOfBirth`, `phone` — no annotations |
| `src/main/java/com/medibook/exception/DuplicateEmailException.java` | New `RuntimeException`, same shape as `DoubleBookingException.java` |
| `src/main/java/com/medibook/service/PatientService.java` | `@Service @Transactional`, all business rules |
| `src/main/java/com/medibook/controller/PatientController.java` | `@RestController @RequestMapping("/api/v1/patients")`, thin pass-through |

## File to modify

`src/main/java/com/medibook/config/GlobalExceptionHandler.java` — add one handler:

```java
@ExceptionHandler(DuplicateEmailException.class)
ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), req);
}
```

Placed next to `handleDoubleBooking` (both 409s), plus the import. No other handler changes needed —
`handleValidation` already generically covers any new `@Valid` DTO failure → 400, and `handleNotFound`
already includes `PatientNotFoundException` → 404.

`Patient.java` and `PatientRepository.java` are **not modified** — reused exactly as-is.

## DTO validation (with rationale)

**`RegisterPatientRequest(String name, String email, LocalDate dateOfBirth, String phone)`**
- `name`: `@NotBlank @Size(max = 255)` — blank rejected cleanly (400) instead of hitting the DB's
  `NOT NULL` constraint; `max=255` matches Hibernate's default `varchar(255)` DDL so an oversized
  value fails as a clean 400 instead of an ugly DB-level error.
- `email`: `@NotBlank @Email @Size(max = 255)` — `@Email` is what satisfies the README's "invalid
  email → 400" clause.
- `dateOfBirth`: `@NotNull @PastOrPresent` — **not** `@Past`. A newborn registered on their actual
  birth date has `dateOfBirth == LocalDate.now()`, which `@Past` would incorrectly reject.
- `phone`: `@NotBlank @Size(max = 255)` — no `@Pattern` (confirmed decision #4).

**`UpdatePatientRequest(String name, String email, String phone)`** — identical annotations, DOB
field omitted entirely (confirmed decision #2).

**`PatientResponse(Long id, String name, String email, LocalDate dateOfBirth, String phone)`** — no
validation annotations, output-only.

## `PatientService` — logic

Constructor-injects `PatientRepository` only. Private helper `normalizeEmail(String)` = trim +
`toLowerCase(Locale.ROOT)`, used everywhere an email is read from a request. Private
`toResponse(Patient)` mapper at the end of the file, matching `AppointmentService`'s convention.

**`registerPatient(RegisterPatientRequest request)`**
1. Normalize email.
2. Pre-check: `patientRepository.findByEmail(normalized).ifPresent(p -> { throw new DuplicateEmailException(normalized); })`.
3. Build `new Patient(name, normalizedEmail, dateOfBirth, phone)`.
4. Save wrapped in a `try/catch (DataIntegrityViolationException)` → rethrow as `DuplicateEmailException`.
   This closes the race window between step 2's check and the actual insert: two concurrent
   registrations with the same email could both pass the pre-check before either commits: the
   DB's existing `@Column(unique = true)` constraint is the real backstop, and this catch converts
   the resulting `DataIntegrityViolationException` into the same 409 instead of a 500.
5. Return `toResponse(saved)`.

**`listPatients()`**
- `patientRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(this::toResponse).toList()`.
  `findAll(Sort)` is inherited from `JpaRepository` — no repository change needed. No pagination,
  no filter (patients aren't filterable per the README — only doctors are).

**`getPatient(Long id)`**
- `patientRepository.findById(id).orElseThrow(() -> new PatientNotFoundException(id))` → `toResponse`.

**`updatePatient(Long id, UpdatePatientRequest request)`**
1. Load patient or throw `PatientNotFoundException`.
2. Normalize the new email.
3. Self-exclusion check (since `PatientRepository` has no `excludeId`-style query and must stay
   unmodified): `findByEmail(normalized).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> { throw new DuplicateEmailException(normalized); })`.
   This prevents a false 409 when a patient is updated without actually changing their email.
4. `patient.setName(...); patient.setEmail(normalized); patient.setPhone(...);` — DOB untouched.
5. Save, wrapped in the same `DataIntegrityViolationException → DuplicateEmailException` fallback
   as `registerPatient` (covers the same race window between step 3 and the save).
6. Return `toResponse(saved)`.

## `PatientController` — endpoints

| Method | Path | Body | Response | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/patients` | `RegisterPatientRequest` | `PatientResponse` | `201` (`@ResponseStatus(HttpStatus.CREATED)`) |
| `GET` | `/api/v1/patients` | — | `List<PatientResponse>` | `200` |
| `GET` | `/api/v1/patients/{id}` | — | `PatientResponse` | `200` |
| `PUT` | `/api/v1/patients/{id}` | `UpdatePatientRequest` | `PatientResponse` | `200` |

Thin controller, no `ResponseEntity`, `@Valid @RequestBody` on both write endpoints, exceptions
bubble to `GlobalExceptionHandler` untouched — same shape as `AppointmentController.java`.

## Tests (TDD — write failing tests first, per CLAUDE.md)

**`src/test/java/com/medibook/service/PatientServiceTest.java`** — `@ExtendWith(MockitoExtension.class)`,
`@Mock PatientRepository`, `new PatientService(patientRepository)` in `@BeforeEach`, AssertJ,
grouped by `// ── section ──` comments (mirroring `AppointmentServiceTest.java`):

- register: `register_happyPath_returnsSavedPatientResponse`,
  `register_normalizesEmailToLowercaseBeforeLookupAndSave`,
  `register_rejectsDuplicateEmail_onPreCheck` (verify `save` never called),
  `register_rejectsDuplicateEmail_onSaveTimeRaceCondition` (mock `save` throwing
  `DataIntegrityViolationException`), `register_duplicateEmailCheckIsCaseInsensitive`.
- list: `list_returnsAllPatientsMappedToResponse`, `list_returnsEmptyListWhenNoPatients`,
  `list_requestsIdAscendingSortOrder`.
- get: `get_happyPath_returnsPatientResponse`, `get_rejectsUnknownId`.
- update: `update_happyPath_updatesNameEmailPhone`, `update_rejectsUnknownId`,
  `update_allowsUnchangedEmail_doesNotSelfConflict`,
  `update_rejectsEmailAlreadyUsedByAnotherPatient`,
  `update_rejectsDuplicateEmail_onSaveTimeRaceCondition`,
  `update_normalizesEmailToLowercaseBeforeLookupAndSave`,
  `update_doesNotChangeDateOfBirth`.

**`src/test/java/com/medibook/controller/PatientControllerTest.java`** — `@WebMvcTest(PatientController.class)`,
`@Autowired MockMvc`, `@Autowired tools.jackson.databind.ObjectMapper` (Jackson 3 package, not
`com.fasterxml`), `@MockitoBean PatientService`:

- POST: `postPatient_validRequest_returns201AndBody`, `postPatient_missingName_returns400`,
  `postPatient_blankName_returns400`, `postPatient_missingEmail_returns400`,
  `postPatient_malformedEmail_returns400`, `postPatient_missingDateOfBirth_returns400`,
  `postPatient_futureDateOfBirth_returns400`, `postPatient_missingPhone_returns400`,
  `postPatient_oversizedName_returns400`, `postPatient_serviceThrowsDuplicateEmail_returns409`,
  `postPatient_errorBodyContainsAllRequiredFields` (full jsonPath assertions on the 409 case,
  mirroring the `$.timestamp`/`$.status`/`$.error`/`$.message`/`$.path` pattern in
  `AppointmentControllerTest.java`).
- GET list: `getPatients_returnsListWith200`, `getPatients_returnsEmptyListWith200WhenNoPatients`.
- GET one: `getPatient_existingId_returns200WithBody`, `getPatient_unknownId_returns404`.
- PUT: `putPatient_validRequest_returns200`, `putPatient_missingEmail_returns400`,
  `putPatient_malformedEmail_returns400`, `putPatient_serviceThrowsDuplicateEmail_returns409`,
  `putPatient_serviceThrowsPatientNotFound_returns404`.

**`src/test/java/com/medibook/repository/PatientRepositoryTest.java`** — `@DataJpaTest` (relocated
Spring Boot 4 packages: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
`org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`), fixtures via
`em.persistAndFlush(...)`:

- `findByEmail_returnsPatientWhenExists`, `findByEmail_returnsEmptyWhenNotExists`.
- `save_rejectsDuplicateEmailAtDbLevel_throwsDataIntegrityViolation` — persist a patient, then
  save+flush a second one with the identical email string; proves the DB constraint that
  `PatientService`'s race-condition fallback depends on actually exists.
- `findByEmail_isCaseSensitiveAtRawRepositoryLevel` — persist `"john@test.com"`, look up
  `"John@Test.com"`, assert empty — documents that case-insensitivity is enforced only by the
  service's normalization, never rely on it at the repository layer.

**Extend existing `src/test/java/com/medibook/config/GlobalExceptionHandlerTest.java`** — add a
`duplicateEmail()` stub throwing `DuplicateEmailException("john@test.com")` to the existing
`StubController`, plus a `duplicateEmailException_returns409WithFullErrorBody` test in the
`// ── 409 ──` section, mirroring `doubleBookingException_returns409WithFullErrorBody`.

## Verification

1. `./mvnw test` — all new and existing tests must pass (currently 50 tests pass; expect the
   count to grow by roughly the test list above).
2. `./mvnw spring-boot:run` (Postgres container must be up — `docker compose up -d`, port 5433 per
   the current `application.properties`), then manually exercise the golden path:
   - `POST /api/v1/patients` with a valid body → 201 + body.
   - `POST /api/v1/patients` again with the same email (different case) → 409 `DUPLICATE_EMAIL`.
   - `GET /api/v1/patients` → 200, includes the new patient.
   - `GET /api/v1/patients/{id}` → 200.
   - `PUT /api/v1/patients/{id}` changing only the phone → 200, email/name unchanged, no false 409.
   - `PUT /api/v1/patients/{id}` with an email belonging to a different patient → 409.
   - `POST /api/v1/patients` with a malformed email / blank name / future DOB → 400 with a
     structured `ErrorResponse` body in each case.

---

## Post-implementation note

This plan has since been fully implemented and verified (92/92 tests passing, manual smoke test
against the running app confirmed all scenarios above including the newborn-DOB edge case and
update self-exclusion). See `src/main/java/com/medibook/service/PatientService.java` and
`src/main/java/com/medibook/controller/PatientController.java` for the final code.
