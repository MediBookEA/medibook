# Testing conventions

Read this when writing or reviewing tests.

## The pyramid, and what each level is for

| Level | Tool | Asserts |
| --- | --- | --- |
| Unit | JUnit 5 + Mockito | one domain rule, repository mocked |
| Integration | Testcontainers (real Postgres) + `@SpringBootTest` | the rule survives the DB (constraints, transactions, concurrency) |
| API | MockMvc / RestAssured | status codes, JSON shape, validation errors |
| E2E | Playwright | the user can actually complete the flow |

## Naming

`methodUnderTest_condition_expectedOutcome`

```
book_whenDoctorAlreadyHasOverlappingAppointment_throwsSlotUnavailable
book_whenSlotIsFree_persistsAppointment
cancel_whenCallerIsNotTheOwner_throwsForbidden
```

## Rules

- **Never** write an assertion that cannot fail. `assertNotNull(result)` alone is
  not a test.
- **Never** weaken or delete a test to make a build green. If a test fails, the
  code is wrong until proven otherwise — say so and stop.
- Every domain rule needs a test for the case where the rule **rejects**
  something, not just the happy path. The rejection case *is* the feature.
- Time is injected (`java.time.Clock` bean), never `Instant.now()` inline —
  otherwise tests are unrepeatable and DST bugs are untestable.
- Concurrency rules (double-booking) need a test that fires N parallel requests
  and asserts exactly one succeeds.

## Test-first (TDD) mode

When the user asks for TDD, or when adding a domain rule:

1. Write the failing test. Run it. **Show that it is red**, and quote the failure.
2. Do not touch the implementation in the same step.
3. Then implement the minimum to go green.
4. You may **not** modify the test to make it pass. If the test looks wrong, ask.

## Coverage

JaCoCo. Service layer must stay above 85% line coverage — but never chase the
number with meaningless tests; cover branches that encode a rule.