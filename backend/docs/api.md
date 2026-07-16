# MediBook API Reference

Base URL: `/api/v1`

## Appointments

### Book an appointment

**Endpoint:** `POST /appointments`

**Description:** Patient books a 30-minute appointment with a doctor. The appointment must start on the hour or half-hour, fall within the doctor's working hours, and be in the future. Neither the patient nor the doctor can have overlapping booked appointments.

**Request body:**

```json
{
  "patientId": 1,
  "doctorId": 2,
  "startTime": "2026-07-21T09:00:00"
}
```

**Success response:** `201 Created`

```json
{
  "id": 5,
  "patientId": 1,
  "patientName": "Alice Smith",
  "doctorId": 2,
  "doctorName": "Dr. Johnson",
  "startTime": "2026-07-21T09:00:00",
  "endTime": "2026-07-21T09:30:00",
  "status": "BOOKED"
}
```

**Error responses:**

- `400 Bad Request`: Missing or invalid required fields
- `404 Not Found`: Patient or doctor ID does not exist
- `409 Conflict`: Double booking — patient or doctor already has an appointment at that time
- `422 Unprocessable Content`: Appointment outside working hours, in the past, or does not start on the hour/half-hour

**Example cURL:**

```bash
curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": 1,
    "doctorId": 2,
    "startTime": "2026-07-21T09:00:00"
  }'
```

---

### Cancel an appointment

**Endpoint:** `DELETE /appointments/{id}`

**Description:** Cancel an appointment. Sets the appointment status to CANCELLED. The time slot becomes available for rebooking.

**Path parameters:**

- `id` (Long, required): Appointment ID

**Success response:** `200 OK`

```json
{
  "id": 5,
  "patientId": 1,
  "patientName": "Alice Smith",
  "doctorId": 2,
  "doctorName": "Dr. Johnson",
  "startTime": "2026-07-21T09:00:00",
  "endTime": "2026-07-21T09:30:00",
  "status": "CANCELLED"
}
```

**Error responses:**

- `404 Not Found`: Appointment does not exist

**Example cURL:**

```bash
curl -X DELETE http://localhost:8080/api/v1/appointments/5
```

---

### Reschedule an appointment

**Endpoint:** `PUT /appointments/{id}`

**Description:** Move an appointment to a different time slot. The new time must satisfy all booking validation rules (future, on the hour/half-hour, within working hours, no conflicts).

**Path parameters:**

- `id` (Long, required): Appointment ID

**Request body:**

```json
{
  "startTime": "2026-07-22T14:00:00"
}
```

**Success response:** `200 OK`

```json
{
  "id": 5,
  "patientId": 1,
  "patientName": "Alice Smith",
  "doctorId": 2,
  "doctorName": "Dr. Johnson",
  "startTime": "2026-07-22T14:00:00",
  "endTime": "2026-07-22T14:30:00",
  "status": "BOOKED"
}
```

**Error responses:**

- `400 Bad Request`: Missing or invalid `startTime`
- `404 Not Found`: Appointment does not exist
- `409 Conflict`: New slot conflicts with another booking
- `422 Unprocessable Content`: New slot is outside working hours, in the past, or does not start on the hour/half-hour

**Example cURL:**

```bash
curl -X PUT http://localhost:8080/api/v1/appointments/5 \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2026-07-22T14:00:00"
  }'
```

---

### Get doctor's schedule by date

**Endpoint:** `GET /appointments?doctorId={doctorId}&date={date}`

**Description:** Retrieve all booked appointments for a specific doctor on a specific date. Cancelled appointments are excluded.

**Query parameters:**

- `doctorId` (Long, required): Doctor ID
- `date` (LocalDate, required): Date in `YYYY-MM-DD` format

**Success response:** `200 OK`

```json
[
  {
    "id": 5,
    "patientId": 1,
    "patientName": "Alice Smith",
    "doctorId": 2,
    "doctorName": "Dr. Johnson",
    "startTime": "2026-07-21T09:00:00",
    "endTime": "2026-07-21T09:30:00",
    "status": "BOOKED"
  },
  {
    "id": 6,
    "patientId": 3,
    "patientName": "Bob Brown",
    "doctorId": 2,
    "doctorName": "Dr. Johnson",
    "startTime": "2026-07-21T10:00:00",
    "endTime": "2026-07-21T10:30:00",
    "status": "BOOKED"
  }
]
```

**Error responses:**

- `404 Not Found`: Doctor does not exist
- `400 Bad Request`: Missing or invalid query parameters

**Example cURL:**

```bash
curl -X GET "http://localhost:8080/api/v1/appointments?doctorId=2&date=2026-07-21"
```

---

### Get doctor's upcoming schedule

**Endpoint:** `GET /appointments?doctorId={doctorId}&from={from}`

**Description:** Retrieve all booked appointments for a doctor from a specific date onward. Cancelled appointments are excluded.

**Query parameters:**

- `doctorId` (Long, required): Doctor ID
- `from` (LocalDate, required): Start date (inclusive) in `YYYY-MM-DD` format

**Success response:** `200 OK`

```json
[
  {
    "id": 5,
    "patientId": 1,
    "patientName": "Alice Smith",
    "doctorId": 2,
    "doctorName": "Dr. Johnson",
    "startTime": "2026-07-21T09:00:00",
    "endTime": "2026-07-21T09:30:00",
    "status": "BOOKED"
  }
]
```

**Error responses:**

- `404 Not Found`: Doctor does not exist
- `400 Bad Request`: Missing or invalid query parameters

**Example cURL:**

```bash
curl -X GET "http://localhost:8080/api/v1/appointments?doctorId=2&from=2026-07-21"
```

---

## Patients

### Register a patient

**Endpoint:** `POST /patients`

**Description:** Register a new patient in the system.

**Request body:**

```json
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "phone": "+1-555-0101"
}
```

**Success response:** `201 Created`

```json
{
  "id": 1,
  "name": "Alice Smith",
  "email": "alice@example.com",
  "phone": "+1-555-0101"
}
```

**Error responses:**

- `400 Bad Request`: Missing or invalid required fields
- `409 Conflict`: Email already registered

**Example cURL:**

```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "email": "alice@example.com",
    "phone": "+1-555-0101"
  }'
```

---

### List all patients

**Endpoint:** `GET /patients`

**Description:** Retrieve all registered patients.

**Success response:** `200 OK`

```json
[
  {
    "id": 1,
    "name": "Alice Smith",
    "email": "alice@example.com",
    "phone": "+1-555-0101"
  },
  {
    "id": 2,
    "name": "Bob Brown",
    "email": "bob@example.com",
    "phone": "+1-555-0102"
  }
]
```

**Example cURL:**

```bash
curl -X GET http://localhost:8080/api/v1/patients
```

---

### Get a single patient

**Endpoint:** `GET /patients/{id}`

**Description:** Retrieve a patient by ID.

**Path parameters:**

- `id` (Long, required): Patient ID

**Success response:** `200 OK`

```json
{
  "id": 1,
  "name": "Alice Smith",
  "email": "alice@example.com",
  "phone": "+1-555-0101"
}
```

**Error responses:**

- `404 Not Found`: Patient does not exist

**Example cURL:**

```bash
curl -X GET http://localhost:8080/api/v1/patients/1
```

---

### Update a patient

**Endpoint:** `PUT /patients/{id}`

**Description:** Update patient information. All fields are optional; only provided fields are updated.

**Path parameters:**

- `id` (Long, required): Patient ID

**Request body:**

```json
{
  "name": "Alice Johnson",
  "email": "alice.johnson@example.com",
  "phone": "+1-555-0103"
}
```

**Success response:** `200 OK`

```json
{
  "id": 1,
  "name": "Alice Johnson",
  "email": "alice.johnson@example.com",
  "phone": "+1-555-0103"
}
```

**Error responses:**

- `404 Not Found`: Patient does not exist
- `409 Conflict`: Email is already registered to another patient

**Example cURL:**

```bash
curl -X PUT http://localhost:8080/api/v1/patients/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Johnson",
    "email": "alice.johnson@example.com"
  }'
```

---

## Doctors

### List all doctors

**Endpoint:** `GET /doctors`

**Description:** Retrieve all available doctors. Doctors are read-only seed data; use this endpoint to find available providers.

**Success response:** `200 OK`

```json
[
  {
    "id": 1,
    "name": "Dr. Johnson",
    "specialization": "General Practice",
    "workingHours": [
      {
        "dayOfWeek": "MONDAY",
        "startTime": "09:00:00",
        "endTime": "17:00:00"
      },
      {
        "dayOfWeek": "TUESDAY",
        "startTime": "09:00:00",
        "endTime": "17:00:00"
      }
    ]
  }
]
```

**Example cURL:**

```bash
curl -X GET http://localhost:8080/api/v1/doctors
```

---

## Error Response Format

All error responses follow this standard format:

```json
{
  "timestamp": "2026-07-15T14:30:00",
  "status": 409,
  "error": "DOUBLE_BOOKING",
  "message": "Dr. Johnson already has an appointment at 2026-07-21T09:00",
  "path": "/api/v1/appointments"
}
```

**Fields:**

- `timestamp` (LocalDateTime): When the error occurred
- `status` (int): HTTP status code
- `error` (String): Machine-readable error code
- `message` (String): Human-readable error description
- `path` (String): The request path that triggered the error
