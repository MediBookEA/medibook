package com.medibook.controller;

import com.medibook.api.dto.PatientResponse;
import com.medibook.api.dto.RegisterPatientRequest;
import com.medibook.api.dto.UpdatePatientRequest;
import com.medibook.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        return patientService.registerPatient(request);
    }

    @GetMapping
    public List<PatientResponse> listPatients() {
        return patientService.listPatients();
    }

    @GetMapping("/{id}")
    public PatientResponse getPatient(@PathVariable Long id) {
        return patientService.getPatient(id);
    }

    @PutMapping("/{id}")
    public PatientResponse updatePatient(@PathVariable Long id, @Valid @RequestBody UpdatePatientRequest request) {
        return patientService.updatePatient(id, request);
    }
}
