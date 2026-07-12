package com.medibook.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDateTime startTime;

    // Always startTime + 30 minutes; stored so JPQL overlap queries stay database-portable.
    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    protected Appointment() {}

    public Appointment(Patient patient, Doctor doctor,
                       LocalDateTime startTime, LocalDateTime endTime,
                       AppointmentStatus status) {
        this.patient = patient;
        this.doctor = doctor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AppointmentStatus getStatus() { return status; }

    public void setStatus(AppointmentStatus status) { this.status = status; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
