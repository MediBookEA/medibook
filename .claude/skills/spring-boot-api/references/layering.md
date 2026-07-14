# Layering — worked example

Read this only when creating or reviewing a controller/service pair.

## Package layout

```
com.example.booking
├── api/          controllers + request/response DTOs
├── service/      domain rules
├── domain/       JPA entities + value objects
├── repository/   Spring Data interfaces
└── config/       security, OpenAPI, clock
```

## Good

```java
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
class AppointmentController {

    private final AppointmentService service;   // one collaborator
    private final AppointmentMapper mapper;

    @PostMapping
    @Operation(summary = "Book an appointment")
    ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookRequest req,
                                             @AuthenticationPrincipal Principal me) {
        Appointment created = service.book(me.patientId(), req.doctorId(), req.startTime());
        return ResponseEntity.status(CREATED).body(mapper.toResponse(created));
    }
}
```

The controller does four things and nothing else: bind, validate, delegate, map.

## Bad (this is what we are demoing as the "before")

```java
@GetMapping("/{id}")
Appointment get(@PathVariable UUID id) {
    return repository.findById(id).orElseThrow();   // 3 violations
}
```

1. Controller reaches into the repository — skips the service layer.
2. Returns the JPA **entity** — leaks the schema, triggers lazy-load errors.
3. **No ownership check** — patient A can read patient B's appointment (IDOR).

## Service layer

All rules live here. Example — the double-booking rule:

```java
@Transactional
public Appointment book(UUID patientId, UUID doctorId, ZonedDateTime start) {
    Instant from = start.toInstant();
    Instant to   = from.plus(SLOT);

    if (repository.existsOverlapping(doctorId, from, to))
        throw new SlotUnavailableException(doctorId, start);

    if (repository.existsOverlappingForPatient(patientId, from, to))
        throw new PatientDoubleBookedException(patientId, start);

    return repository.save(Appointment.of(patientId, doctorId, from, to));
}
```

The overlap check is *also* enforced by a DB exclusion constraint — the
application check gives a nice error, the constraint guarantees correctness
under concurrency. Both, always.

## Errors

Domain exceptions extend `DomainException` and are translated once, centrally:

```java
@ExceptionHandler(SlotUnavailableException.class)
ProblemDetail onSlotUnavailable(SlotUnavailableException e) {
    var pd = ProblemDetail.forStatus(CONFLICT);          // 409
    pd.setTitle("Slot unavailable");
    pd.setDetail("That time is already booked.");         // no PHI in the message
    return pd;
}
```