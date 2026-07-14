package com.medibook.api.dto;

import java.util.List;

public record DoctorResponse(
        Long id,
        String name,
        String specialty,
        List<WorkingHoursResponse> workingHours
) {}
