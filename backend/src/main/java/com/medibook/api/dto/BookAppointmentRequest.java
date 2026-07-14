package com.medibook.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookAppointmentRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        @NotNull LocalDateTime startTime
) {}
