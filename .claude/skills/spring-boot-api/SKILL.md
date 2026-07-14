---
name: spring-boot-api
description: Build, extend or refactor REST APIs in this Spring Boot healthcare service. Use this skill whenever the user asks for a new endpoint, controller, service, repository, DTO, request/response model, validation rule, exception handler, Flyway migration, or any change to the backend API surface — even if they don't say "Spring Boot" and even if they only describe the behaviour ("patients should be able to cancel a booking"). Also use it when reviewing backend code for layering or convention violations.
---

# Spring Boot API (healthcare booking service)

Our conventions. Follow them exactly — matching the existing codebase matters
more than any general Spring best practice you may know.

## Stack

Java 21 · Spring Boot 4.x · PostgreSQL · Flyway · MapStruct · JUnit 5 + Mockito ·
Testcontainers · Maven (`./mvnw`, never a globally installed `mvn`).

## The one rule that matters

Requests flow **strictly downhill** and never skip a layer:

```
Controller  ->  Service  ->  Repository  ->  DB
  (HTTP)      (all rules)     (queries)
```

- A **controller** validates input, calls exactly one service method, maps to a
  response DTO. It contains **zero** business logic and **never** touches a
  repository.
- A **service** owns every domain rule (overlap checks, cancellation windows,
  ownership checks). It is the only layer allowed to be interesting.
- A **repository** is a Spring Data interface. Custom queries use `@Query` with
  named parameters.

Details and a worked example: read `references/layering.md`.

## Non-negotiables

| Rule | Why |
| --- | --- |
| Never return a JPA entity from a controller | leaks the schema + lazy-loading blowups |
| Constructor injection only, fields `private final` | testability; no field `@Autowired` |
| Every write endpoint validates with `@Valid` + Jakarta annotations | |
| Errors go through `GlobalExceptionHandler` -> RFC 7807 `ProblemDetail` | never a raw stack trace to the client |
| Flyway migrations are **append-only** | editing `V3__*.sql` breaks every deployed env — add `V4__*.sql` |
| **Never log PHI** — no patient name, DOB, email, SSN. Log the UUID only. | this is a healthcare app |
| Timestamps are `Instant` in the DB, `ZonedDateTime` at the boundary | DST is where double-booking bugs live |

## Ownership / authorization

Every endpoint that reads or writes an appointment **must** verify that the
authenticated principal owns the resource, in the *service* layer. An endpoint
that fetches by ID without an ownership check is an IDOR vulnerability, not a
shortcut. If the user asks for such an endpoint, add the check anyway and say so.

## Definition of done

An API change is not finished until:

1. `./mvnw -q verify` is green.
2. Unit tests cover the happy path **and** every domain rule you added
   (especially the failing/conflict case).
3. There is one integration test hitting the real DB via Testcontainers.
4. The endpoint is annotated for OpenAPI (`@Operation`, `@ApiResponse`).

Test patterns and naming: read `references/testing.md`.

## Preferred workflow

1. Read the nearest existing slice before writing anything.
2. If the change is non-trivial, propose the file list first, then implement.
3. Write the failing test, then the implementation (see `references/testing.md`).
4. Run `./mvnw -q verify` yourself. Never claim success on an unverified build.