package com.medibook.api.dto;

import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String name,
        String email,
        LocalDate dateOfBirth,
        String phone
) {}
