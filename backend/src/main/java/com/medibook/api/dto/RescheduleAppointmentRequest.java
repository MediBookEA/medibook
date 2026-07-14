package com.medibook.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(
        @NotNull LocalDateTime startTime
) {}
