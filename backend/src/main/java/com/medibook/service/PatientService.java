package com.medibook.service;

import com.medibook.api.dto.PatientResponse;
import com.medibook.api.dto.RegisterPatientRequest;
import com.medibook.api.dto.UpdatePatientRequest;
import com.medibook.domain.Patient;
import com.medibook.exception.DuplicateEmailException;
import com.medibook.exception.PatientNotFoundException;
import com.medibook.repository.PatientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public PatientResponse registerPatient(RegisterPatientRequest request) {
        String email = normalizeEmail(request.email());
        patientRepository.findByEmail(email)
                .ifPresent(p -> { throw new DuplicateEmailException(email); });

        Patient patient = new Patient(request.name(), email, request.dateOfBirth(), request.phone());
        Patient saved = saveOrThrowDuplicate(patient, email);
        return toResponse(saved);
    }

    public List<PatientResponse> listPatients() {
        return patientRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    public PatientResponse getPatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        return toResponse(patient);
    }

    public PatientResponse updatePatient(Long id, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        String email = normalizeEmail(request.email());
        patientRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new DuplicateEmailException(email); });

        patient.setName(request.name());
        patient.setEmail(email);
        patient.setPhone(request.phone());

        Patient saved = saveOrThrowDuplicate(patient, email);
        return toResponse(saved);
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private Patient saveOrThrowDuplicate(Patient patient, String email) {
        try {
            return patientRepository.save(patient);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(email);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(p.getId(), p.getName(), p.getEmail(), p.getDateOfBirth(), p.getPhone());
    }
}
