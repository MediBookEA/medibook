# Clinic Appointment System

## Features 

- Patient Registry 
    - Register a patient 
        - Name, email, date of birth, phone number
    - View/list patients
    - Update contact info
- Doctor directory
    - Register a doctor
        - Name, specialty, and weekly working hours
    - View/list doctors 
    - Update doctor info
- Appointment booking
    - Book a 30-minute slot for a patient with a doctor. Business rules: no double-booking a doctor, no double-booking a patient, must fall inside the doctor's working hours, must be in the future.

-  Cancel & reschedule. Cancel sets status to CANCELLED (no hard delete); reschedule = validate new slot with the same rules. Small feature, but it forces refactoring (extracting the slot-validation logic into one place), which demos Claude-driven refactoring and rewind/checkpoints nicely.

## Functional requirements
    - The system shall register a patient with name, email (unique), date of birth, and phone; an invalid or duplicate email is rejected with a 400/409 and a structured error body.
    - The system shall list and retrieve patients and doctors; doctors are filterable by specialty.
    - The system shall book a 30-minute appointment given the patient ID, doctor ID, and start time.
    - The system shall reject a booking that overlaps an existing non-cancelled appointment for the same doctor or the same patient (409 Conflict).
    - The system shall reject a booking outside the doctor's working hours or in the past (422).
    - The system shall allow cancelling an appointment (status → CANCELLED); cancelled slots become bookable again.
    - The system shall allow rescheduling an appointment to a new slot, applying the same validation as booking.
    - The system shall return a doctor's day schedule as a list of slots marked free or booked.
    - The system shall return a patient's upcoming appointments sorted by start time.

