package com.medibook.api.dto;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {}
