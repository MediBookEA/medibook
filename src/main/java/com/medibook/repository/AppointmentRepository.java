package com.medibook.repository;

import com.medibook.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctor.id = :doctorId
              AND a.status = com.medibook.domain.AppointmentStatus.BOOKED
              AND a.startTime < :endTime
              AND a.endTime   > :startTime
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    List<Appointment> findOverlappingForDoctor(
            @Param("doctorId")  Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime")   LocalDateTime endTime,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.patient.id = :patientId
              AND a.status = com.medibook.domain.AppointmentStatus.BOOKED
              AND a.startTime < :endTime
              AND a.endTime   > :startTime
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    List<Appointment> findOverlappingForPatient(
            @Param("patientId") Long patientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime")   LocalDateTime endTime,
            @Param("excludeId") Long excludeId);
}
