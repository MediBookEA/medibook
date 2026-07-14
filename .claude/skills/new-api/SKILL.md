---
name: new-api
description: Scaffold a new Spring Boot REST API resource (controller, service, repository, DTOs, tests)
argument-hint: <resource-name> [extra requirements]
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./mvnw:*), Bash(git status:*), Bash(git diff:*)
disable-model-invocation: true
---

# Create a new API resource: $1

Create a complete, production-ready REST resource for **$1** in this Spring Boot project.

Full request from the developer: `$ARGUMENTS`

## Steps

1. **Explore first, write second.** Read an existing vertical slice
   (`AppointmentController` -> `AppointmentService` -> `AppointmentRepository`)
   so the new code is indistinguishable from the code we already have.
2. **Apply the `spring-boot-api` skill.** It holds our layering, naming,
   validation and testing rules. Follow it exactly — do not invent conventions.
3. **Produce the full slice:**
   - `…/api/<Resource>Controller.java`   (thin — no business logic, no entities)
   - `…/service/<Resource>Service.java`  (all rules live here)
   - `…/repository/<Resource>Repository.java`
   - `…/dto/<Resource>Request.java` / `<Resource>Response.java` (+ MapStruct mapper)
   - a **new** Flyway migration if the schema changes — never edit an existing one
   - unit tests (Mockito) + an integration test (Testcontainers + MockMvc)
4. **Verify.** Run `./mvnw -q verify`. If it is red, fix it. Do not report success
   on a failing build.
5. **Report** with a table: file | layer | what it does. Then list any assumption
   you had to make about the domain.

## Guardrails

- Never return a JPA entity from a controller. DTOs only.
- Never log patient-identifying data (name, DOB, SSN, email). Log the ID only.
- Constructor injection only. No `@Autowired` on fields.
- If `$1` is ambiguous, ask **one** clarifying question before writing any code.