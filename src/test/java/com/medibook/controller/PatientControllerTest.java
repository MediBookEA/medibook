package com.medibook.controller;

import tools.jackson.databind.ObjectMapper;
import com.medibook.api.dto.PatientResponse;
import com.medibook.api.dto.RegisterPatientRequest;
import com.medibook.api.dto.UpdatePatientRequest;
import com.medibook.exception.DuplicateEmailException;
import com.medibook.exception.PatientNotFoundException;
import com.medibook.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private PatientService service;

    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);

    private PatientResponse patientResponse() {
        return new PatientResponse(1L, "John Doe", "john@test.com", DOB, "555-0000");
    }

    private RegisterPatientRequest validRegisterRequest() {
        return new RegisterPatientRequest("John Doe", "john@test.com", DOB, "555-0000");
    }

    private UpdatePatientRequest validUpdateRequest() {
        return new UpdatePatientRequest("John Doe", "john@test.com", "555-0000");
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ── POST /api/v1/patients ─────────────────────────────────────────────────

    @Test
    void postPatient_validRequest_returns201AndBody() throws Exception {
        when(service.registerPatient(any())).thenReturn(patientResponse());

        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void postPatient_missingName_returns400() throws Exception {
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest(null, "john@test.com", DOB, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_blankName_returns400() throws Exception {
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest("   ", "john@test.com", DOB, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_missingEmail_returns400() throws Exception {
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest("John Doe", null, DOB, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_malformedEmail_returns400() throws Exception {
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest("John Doe", "not-an-email", DOB, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_missingDateOfBirth_returns400() throws Exception {
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest("John Doe", "john@test.com", null, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_futureDateOfBirth_returns400() throws Exception {
        LocalDate future = LocalDate.now().plusDays(1);
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest("John Doe", "john@test.com", future, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_missingPhone_returns400() throws Exception {
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest("John Doe", "john@test.com", DOB, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_oversizedName_returns400() throws Exception {
        String oversizedName = "A".repeat(256);
        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterPatientRequest(oversizedName, "john@test.com", DOB, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postPatient_serviceThrowsDuplicateEmail_returns409() throws Exception {
        when(service.registerPatient(any())).thenThrow(new DuplicateEmailException("john@test.com"));

        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegisterRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));
    }

    @Test
    void postPatient_errorBodyContainsAllRequiredFields() throws Exception {
        when(service.registerPatient(any())).thenThrow(new DuplicateEmailException("john@test.com"));

        mvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRegisterRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/patients"));
    }

    // ── GET /api/v1/patients ──────────────────────────────────────────────────

    @Test
    void getPatients_returnsListWith200() throws Exception {
        when(service.listPatients()).thenReturn(List.of(patientResponse()));

        mvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    void getPatients_returnsEmptyListWith200WhenNoPatients() throws Exception {
        when(service.listPatients()).thenReturn(List.of());

        mvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/v1/patients/{id} ─────────────────────────────────────────────

    @Test
    void getPatient_existingId_returns200WithBody() throws Exception {
        when(service.getPatient(1L)).thenReturn(patientResponse());

        mvc.perform(get("/api/v1/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void getPatient_unknownId_returns404() throws Exception {
        when(service.getPatient(99L)).thenThrow(new PatientNotFoundException(99L));

        mvc.perform(get("/api/v1/patients/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ── PUT /api/v1/patients/{id} ─────────────────────────────────────────────

    @Test
    void putPatient_validRequest_returns200() throws Exception {
        when(service.updatePatient(eq(1L), any())).thenReturn(patientResponse());

        mvc.perform(put("/api/v1/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void putPatient_missingEmail_returns400() throws Exception {
        mvc.perform(put("/api/v1/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdatePatientRequest("John Doe", null, "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void putPatient_malformedEmail_returns400() throws Exception {
        mvc.perform(put("/api/v1/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdatePatientRequest("John Doe", "not-an-email", "555-0000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void putPatient_serviceThrowsDuplicateEmail_returns409() throws Exception {
        when(service.updatePatient(eq(1L), any())).thenThrow(new DuplicateEmailException("john@test.com"));

        mvc.perform(put("/api/v1/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"));
    }

    @Test
    void putPatient_serviceThrowsPatientNotFound_returns404() throws Exception {
        when(service.updatePatient(eq(99L), any())).thenThrow(new PatientNotFoundException(99L));

        mvc.perform(put("/api/v1/patients/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
