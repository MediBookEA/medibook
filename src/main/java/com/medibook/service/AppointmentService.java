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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        var slot = validateSlot(request.startTime(), request.patientId(), request.doctorId(), null);
        Appointment saved = appointmentRepository.save(
                new Appointment(slot.patient(), slot.doctor(),
                        request.startTime(), slot.endTime(), AppointmentStatus.BOOKED));
        return toResponse(saved);
    }

    public AppointmentResponse cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse rescheduleAppointment(Long id, RescheduleAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        var slot = validateSlot(request.startTime(),
                appointment.getPatient().getId(), appointment.getDoctor().getId(), id);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(slot.endTime());
        return toResponse(appointmentRepository.save(appointment));
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private record SlotValidation(Patient patient, Doctor doctor, LocalDateTime endTime) {}

    private SlotValidation validateSlot(LocalDateTime startTime,
                                        Long patientId, Long doctorId, Long excludeId) {
        // 1. Must be in the future
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new AppointmentInPastException();
        }
        // 2. Must start on the hour or half-hour, no sub-minute precision
        int minute = startTime.getMinute();
        if ((minute != 0 && minute != 30) || startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new InvalidStartTimeException();
        }
        // 3–4. Resolve entities
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));
        // 5. Compute end time
        LocalDateTime endTime = startTime.plusMinutes(30);
        // 6. Check working hours
        DayOfWeek day = startTime.getDayOfWeek();
        WorkingHours wh = doctor.getWorkingHours().stream()
                .filter(h -> h.getDayOfWeek() == day)
                .findFirst()
                .orElseThrow(() -> new OutsideWorkingHoursException(doctor.getName()));
        LocalTime slotStart = startTime.toLocalTime();
        LocalTime slotEnd   = endTime.toLocalTime();
        if (slotStart.isBefore(wh.getStartTime()) || slotEnd.isAfter(wh.getEndTime())) {
            throw new OutsideWorkingHoursException(doctor.getName());
        }
        // 7. Doctor conflict
        if (!appointmentRepository
                .findOverlappingForDoctor(doctorId, startTime, endTime, excludeId).isEmpty()) {
            throw new DoubleBookingException(doctor.getName(), startTime);
        }
        // 8. Patient conflict
        if (!appointmentRepository
                .findOverlappingForPatient(patientId, startTime, endTime, excludeId).isEmpty()) {
            throw new DoubleBookingException(patient.getName(), startTime);
        }
        return new SlotValidation(patient, doctor, endTime);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(), a.getPatient().getName(),
                a.getDoctor().getId(),  a.getDoctor().getName(),
                a.getStartTime(), a.getEndTime(),
                a.getStatus().name());
    }
}
