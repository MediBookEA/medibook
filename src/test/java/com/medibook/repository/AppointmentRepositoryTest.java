package com.medibook.repository;

import com.medibook.domain.Appointment;
import com.medibook.domain.AppointmentStatus;
import com.medibook.domain.Doctor;
import com.medibook.domain.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AppointmentRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired AppointmentRepository repository;

    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        doctor  = em.persistAndFlush(new Doctor("Dr. Test", "Cardiology"));
        patient = em.persistAndFlush(new Patient("John Doe", "john@test.com",
                LocalDate.of(1990, 1, 1), "555-0000"));
    }

    private Appointment booked(LocalDateTime start) {
        return new Appointment(patient, doctor, start, start.plusMinutes(30), AppointmentStatus.BOOKED);
    }

    private Appointment cancelled(LocalDateTime start) {
        return new Appointment(patient, doctor, start, start.plusMinutes(30), AppointmentStatus.CANCELLED);
    }

    @Test
    void findOverlappingForDoctor_returnsOverlappingBooked() {
        LocalDateTime slot = LocalDateTime.of(2026, 7, 14, 9, 0);
        em.persistAndFlush(booked(slot));

        List<Appointment> result = repository.findOverlappingForDoctor(
                doctor.getId(), slot, slot.plusMinutes(30), null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findOverlappingForDoctor_excludesCancelledAppointments() {
        LocalDateTime slot = LocalDateTime.of(2026, 7, 14, 9, 0);
        em.persistAndFlush(cancelled(slot));

        List<Appointment> result = repository.findOverlappingForDoctor(
                doctor.getId(), slot, slot.plusMinutes(30), null);

        assertThat(result).isEmpty();
    }

    @Test
    void findOverlappingForDoctor_returnsEmptyForBackToBackAppointments() {
        LocalDateTime existing  = LocalDateTime.of(2026, 7, 14, 9, 0);
        LocalDateTime backToBack = existing.plusMinutes(30);
        em.persistAndFlush(booked(existing));

        List<Appointment> result = repository.findOverlappingForDoctor(
                doctor.getId(), backToBack, backToBack.plusMinutes(30), null);

        assertThat(result).isEmpty();
    }

    @Test
    void findOverlappingForDoctor_excludesOwnIdWhenExcludeIdProvided() {
        LocalDateTime slot = LocalDateTime.of(2026, 7, 14, 9, 0);
        Appointment existing = em.persistAndFlush(booked(slot));

        List<Appointment> result = repository.findOverlappingForDoctor(
                doctor.getId(), slot, slot.plusMinutes(30), existing.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findOverlappingForPatient_returnsOverlappingBooked() {
        LocalDateTime slot = LocalDateTime.of(2026, 7, 14, 10, 0);
        em.persistAndFlush(booked(slot));

        List<Appointment> result = repository.findOverlappingForPatient(
                patient.getId(), slot, slot.plusMinutes(30), null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findOverlappingForPatient_returnsEmptyForBackToBackAppointments() {
        LocalDateTime existing  = LocalDateTime.of(2026, 7, 14, 10, 0);
        LocalDateTime backToBack = existing.plusMinutes(30);
        em.persistAndFlush(booked(existing));

        List<Appointment> result = repository.findOverlappingForPatient(
                patient.getId(), backToBack, backToBack.plusMinutes(30), null);

        assertThat(result).isEmpty();
    }
}
