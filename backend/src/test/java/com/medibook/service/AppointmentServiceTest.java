package com.medibook.service;

import com.medibook.api.dto.AppointmentResponse;
import com.medibook.api.dto.BookAppointmentRequest;
import com.medibook.api.dto.RescheduleAppointmentRequest;
import com.medibook.domain.Appointment;
import com.medibook.domain.AppointmentStatus;
import com.medibook.domain.Doctor;
import com.medibook.domain.Patient;
import com.medibook.domain.WorkingHours;
import com.medibook.exception.AppointmentInPastException;
import com.medibook.exception.AppointmentNotFoundException;
import com.medibook.exception.DoctorNotFoundException;
import com.medibook.exception.DoubleBookingException;
import com.medibook.exception.InvalidStartTimeException;
import com.medibook.exception.OutsideWorkingHoursException;
import com.medibook.exception.PatientNotFoundException;
import com.medibook.repository.AppointmentRepository;
import com.medibook.repository.DoctorRepository;
import com.medibook.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private Doctor doctor;
    @Mock private Patient patient;

    private AppointmentService service;
    private WorkingHours mondayHours;

    // 2027-01-04 is a Monday — well past today (2026-07-12)
    private static final LocalDateTime FUTURE_MONDAY_9AM = LocalDateTime.of(2027, 1, 4, 9, 0);
    private static final Long PATIENT_ID = 1L;
    private static final Long DOCTOR_ID  = 2L;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, patientRepository, doctorRepository);
        mondayHours = new WorkingHours(doctor, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        lenient().when(doctor.getId()).thenReturn(DOCTOR_ID);
        lenient().when(doctor.getName()).thenReturn("Dr. Test");
        lenient().when(patient.getId()).thenReturn(PATIENT_ID);
        lenient().when(patient.getName()).thenReturn("John Doe");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private BookAppointmentRequest validRequest() {
        return new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, FUTURE_MONDAY_9AM);
    }

    private void givenValidBookingSetup() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));
        when(appointmentRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findOverlappingForPatient(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    void book_happyPath_returnsSavedAppointmentResponse() {
        givenValidBookingSetup();

        AppointmentResponse response = service.bookAppointment(validRequest());

        assertThat(response.startTime()).isEqualTo(FUTURE_MONDAY_9AM);
        assertThat(response.endTime()).isEqualTo(FUTURE_MONDAY_9AM.plusMinutes(30));
        assertThat(response.status()).isEqualTo("BOOKED");
    }

    @Test
    void cancel_happyPath_setsStatusToCancelled() {
        Appointment existing = new Appointment(patient, doctor,
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.cancelAppointment(10L);

        assertThat(response.status()).isEqualTo("CANCELLED");
    }

    @Test
    void reschedule_happyPath_returnsUpdatedAppointmentResponse() {
        LocalDateTime newSlot = LocalDateTime.of(2027, 1, 4, 10, 0);
        Appointment existing = new Appointment(patient, doctor,
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));
        when(appointmentRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findOverlappingForPatient(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.rescheduleAppointment(10L,
                new RescheduleAppointmentRequest(newSlot));

        assertThat(response.startTime()).isEqualTo(newSlot);
        assertThat(response.status()).isEqualTo("BOOKED");
    }

    // ── future check ─────────────────────────────────────────────────────────

    @Test
    void book_rejectsPastStartTime() {
        LocalDateTime past = LocalDateTime.of(2020, 1, 1, 9, 0);

        assertThatThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, past)))
                .isInstanceOf(AppointmentInPastException.class);
    }

    // ── alignment check ───────────────────────────────────────────────────────

    @Test
    void book_rejectsMinuteNotOnHalfHour() {
        LocalDateTime misaligned = LocalDateTime.of(2027, 1, 4, 9, 15);

        assertThatThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, misaligned)))
                .isInstanceOf(InvalidStartTimeException.class);
    }

    @Test
    void book_rejectsNonZeroSeconds() {
        LocalDateTime withSeconds = LocalDateTime.of(2027, 1, 4, 9, 0, 30);

        assertThatThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, withSeconds)))
                .isInstanceOf(InvalidStartTimeException.class);
    }

    // ── 404s ─────────────────────────────────────────────────────────────────

    @Test
    void book_rejectsUnknownPatientId() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bookAppointment(validRequest()))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void book_rejectsUnknownDoctorId() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bookAppointment(validRequest()))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void cancel_rejectsUnknownId() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelAppointment(99L))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void reschedule_rejectsUnknownId() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rescheduleAppointment(99L,
                new RescheduleAppointmentRequest(FUTURE_MONDAY_9AM)))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    // ── working hours ─────────────────────────────────────────────────────────

    @Test
    void book_rejectsStartBeforeWorkingHoursStart() {
        LocalDateTime tooEarly = LocalDateTime.of(2027, 1, 4, 8, 30);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));

        assertThatThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, tooEarly)))
                .isInstanceOf(OutsideWorkingHoursException.class);
    }

    @Test
    void book_rejectsEndAfterWorkingHoursEnd() {
        // Doctor closes at 16:30; a 16:30 start would end at 17:00 — over the limit
        WorkingHours shortHours = new WorkingHours(doctor, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(16, 30));
        LocalDateTime slot = LocalDateTime.of(2027, 1, 4, 16, 30);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(shortHours));

        assertThatThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, slot)))
                .isInstanceOf(OutsideWorkingHoursException.class);
    }

    @Test
    void book_rejectsStartExactlyAtWorkingHoursEnd() {
        // Start 17:00 → end 17:30; doctor closes at 17:00
        LocalDateTime atEnd = LocalDateTime.of(2027, 1, 4, 17, 0);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));

        assertThatThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, atEnd)))
                .isInstanceOf(OutsideWorkingHoursException.class);
    }

    @Test
    void book_acceptsLastValidSlot() {
        // Start 16:30 → end 17:00; doctor closes at 17:00 — exactly on the boundary
        LocalDateTime lastSlot = LocalDateTime.of(2027, 1, 4, 16, 30);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));
        when(appointmentRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findOverlappingForPatient(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, lastSlot)));
    }

    @Test
    void book_rejectsDayDoctorDoesNotWork() {
        // mondayHours only covers Monday; 2027-01-05 is Tuesday
        LocalDateTime tuesday = LocalDateTime.of(2027, 1, 5, 9, 0);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));

        assertThatThrownBy(() -> service.bookAppointment(
                new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, tuesday)))
                .isInstanceOf(OutsideWorkingHoursException.class);
    }

    // ── doctor double-booking ─────────────────────────────────────────────────

    @Test
    void book_rejectsDoctorDoubleBooking() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));
        // Doctor overlap found → throws before even reaching the patient overlap check
        when(appointmentRepository.findOverlappingForDoctor(any(), any(), any(), any()))
                .thenReturn(List.of(new Appointment(patient, doctor,
                        FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED)));

        assertThatThrownBy(() -> service.bookAppointment(validRequest()))
                .isInstanceOf(DoubleBookingException.class);
    }

    @Test
    void book_allowsDoctorBackToBackAppointments() {
        // Repository returns no overlap (back-to-back check lives in the repository layer)
        givenValidBookingSetup();

        assertThatNoException().isThrownBy(() -> service.bookAppointment(validRequest()));
        verify(appointmentRepository).findOverlappingForDoctor(
                eq(DOCTOR_ID), eq(FUTURE_MONDAY_9AM), eq(FUTURE_MONDAY_9AM.plusMinutes(30)), isNull());
    }

    // ── patient double-booking ────────────────────────────────────────────────

    @Test
    void book_rejectsPatientDoubleBooking() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));
        when(appointmentRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(List.of());
        // Patient overlap found after doctor check passes
        when(appointmentRepository.findOverlappingForPatient(any(), any(), any(), any()))
                .thenReturn(List.of(new Appointment(patient, doctor,
                        FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED)));

        assertThatThrownBy(() -> service.bookAppointment(validRequest()))
                .isInstanceOf(DoubleBookingException.class);
    }

    @Test
    void book_allowsPatientBackToBackAppointments() {
        givenValidBookingSetup();

        assertThatNoException().isThrownBy(() -> service.bookAppointment(validRequest()));
        verify(appointmentRepository).findOverlappingForPatient(
                eq(PATIENT_ID), eq(FUTURE_MONDAY_9AM), eq(FUTURE_MONDAY_9AM.plusMinutes(30)), isNull());
    }

    // ── cancelled slot does not block ─────────────────────────────────────────

    @Test
    void book_allowsRebookingAfterCancellation() {
        // Cancelled slots are invisible to the overlap query (status = BOOKED filter)
        // so the repository returns empty and booking proceeds
        givenValidBookingSetup();

        assertThatNoException().isThrownBy(() -> service.bookAppointment(validRequest()));
    }

    // ── rescheduling self-conflict ────────────────────────────────────────────

    @Test
    void reschedule_doesNotSelfConflict_onOwnCurrentSlot() {
        Long appointmentId = 10L;
        Appointment existing = spy(new Appointment(patient, doctor,
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED));
        doReturn(appointmentId).when(existing).getId();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existing));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));
        when(appointmentRepository.findOverlappingForDoctor(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findOverlappingForPatient(any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() ->
                service.rescheduleAppointment(appointmentId,
                        new RescheduleAppointmentRequest(FUTURE_MONDAY_9AM)));

        // The appointment's own ID must be passed as excludeId so it can't conflict with itself
        verify(appointmentRepository).findOverlappingForDoctor(
                eq(DOCTOR_ID), any(), any(), eq(appointmentId));
        verify(appointmentRepository).findOverlappingForPatient(
                eq(PATIENT_ID), any(), any(), eq(appointmentId));
    }

    @Test
    void reschedule_rejectsConflictWithDifferentAppointment() {
        Long appointmentId = 10L;
        LocalDateTime newSlot = LocalDateTime.of(2027, 1, 4, 10, 0);
        Appointment existing = new Appointment(patient, doctor,
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(existing));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(doctor.getWorkingHours()).thenReturn(List.of(mondayHours));
        when(appointmentRepository.findOverlappingForDoctor(any(), any(), any(), any()))
                .thenReturn(List.of(new Appointment(patient, doctor,
                        newSlot, newSlot.plusMinutes(30), AppointmentStatus.BOOKED)));

        assertThatThrownBy(() -> service.rescheduleAppointment(appointmentId,
                new RescheduleAppointmentRequest(newSlot)))
                .isInstanceOf(DoubleBookingException.class);
    }

    // ── getSchedule ──────────────────────────────────────────────────────────

    @Test
    void getSchedule_rejectsUnknownDoctorId() {
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getSchedule(DOCTOR_ID, LocalDate.of(2026, 7, 14)))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void getSchedule_mapsRepositoryResultsToAppointmentResponseList() {
        LocalDate date = LocalDate.of(2026, 7, 14);
        Appointment appointment = new Appointment(patient, doctor,
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED);
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(appointmentRepository.findBookedForDoctorOnDay(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of(appointment));

        List<AppointmentResponse> result = service.getSchedule(DOCTOR_ID, date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("BOOKED");
        assertThat(result.get(0).patientName()).isEqualTo("John Doe");
        assertThat(result.get(0).doctorName()).isEqualTo("Dr. Test");
    }

    @Test
    void getSchedule_returnsEmptyListWhenRepositoryReturnsEmpty() {
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(appointmentRepository.findBookedForDoctorOnDay(eq(DOCTOR_ID), any(), any()))
                .thenReturn(List.of());

        assertThat(service.getSchedule(DOCTOR_ID, LocalDate.of(2026, 7, 14))).isEmpty();
    }

    @Test
    void getSchedule_passesCorrectDayBoundariesToRepository() {
        LocalDate date = LocalDate.of(2026, 7, 14);
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(appointmentRepository.findBookedForDoctorOnDay(any(), any(), any())).thenReturn(List.of());

        service.getSchedule(DOCTOR_ID, date);

        verify(appointmentRepository).findBookedForDoctorOnDay(
                DOCTOR_ID, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // ── getUpcomingSchedule ──────────────────────────────────────────────────

    @Test
    void getUpcomingSchedule_rejectsUnknownDoctorId() {
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getUpcomingSchedule(DOCTOR_ID, LocalDate.of(2026, 7, 15)))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    void getUpcomingSchedule_mapsRepositoryResultsToAppointmentResponseList() {
        LocalDate from = LocalDate.of(2026, 7, 15);
        Appointment appointment = new Appointment(patient, doctor,
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), AppointmentStatus.BOOKED);
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(appointmentRepository.findBookedForDoctorFrom(eq(DOCTOR_ID), any()))
                .thenReturn(List.of(appointment));

        List<AppointmentResponse> result = service.getUpcomingSchedule(DOCTOR_ID, from);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("BOOKED");
        assertThat(result.get(0).patientName()).isEqualTo("John Doe");
        assertThat(result.get(0).doctorName()).isEqualTo("Dr. Test");
    }

    @Test
    void getUpcomingSchedule_returnsEmptyListWhenRepositoryReturnsEmpty() {
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(appointmentRepository.findBookedForDoctorFrom(eq(DOCTOR_ID), any()))
                .thenReturn(List.of());

        assertThat(service.getUpcomingSchedule(DOCTOR_ID, LocalDate.of(2026, 7, 15))).isEmpty();
    }

    @Test
    void getUpcomingSchedule_passesFromStartOfDayToRepository() {
        LocalDate from = LocalDate.of(2026, 7, 15);
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(appointmentRepository.findBookedForDoctorFrom(any(), any())).thenReturn(List.of());

        service.getUpcomingSchedule(DOCTOR_ID, from);

        verify(appointmentRepository).findBookedForDoctorFrom(DOCTOR_ID, from.atStartOfDay());
    }

    @Test
    void getUpcomingSchedule_ordersResultsAscendingAsReturnedByRepository() {
        LocalDate from = LocalDate.of(2026, 7, 15);
        LocalDateTime earlier = LocalDateTime.of(2026, 7, 16, 9, 0);
        LocalDateTime later = LocalDateTime.of(2026, 7, 20, 9, 0);
        Appointment first = new Appointment(patient, doctor, earlier, earlier.plusMinutes(30), AppointmentStatus.BOOKED);
        Appointment second = new Appointment(patient, doctor, later, later.plusMinutes(30), AppointmentStatus.BOOKED);
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(appointmentRepository.findBookedForDoctorFrom(eq(DOCTOR_ID), any()))
                .thenReturn(List.of(first, second));

        List<AppointmentResponse> result = service.getUpcomingSchedule(DOCTOR_ID, from);

        assertThat(result).extracting(AppointmentResponse::startTime).containsExactly(earlier, later);
    }
}
