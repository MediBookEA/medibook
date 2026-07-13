package com.medibook.service;

import com.medibook.api.dto.PatientResponse;
import com.medibook.api.dto.RegisterPatientRequest;
import com.medibook.api.dto.UpdatePatientRequest;
import com.medibook.domain.Patient;
import com.medibook.exception.DuplicateEmailException;
import com.medibook.exception.PatientNotFoundException;
import com.medibook.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;

    private PatientService service;

    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);

    @BeforeEach
    void setUp() {
        service = new PatientService(patientRepository);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private RegisterPatientRequest validRegisterRequest() {
        return new RegisterPatientRequest("John Doe", "john@test.com", DOB, "555-0000");
    }

    private Patient spyPatient(Long id, String name, String email, String phone) {
        Patient patient = spy(new Patient(name, email, DOB, phone));
        lenient().doReturn(id).when(patient).getId();
        return patient;
    }

    // ── register ────────────────────────────────────────────────────────────

    @Test
    void register_happyPath_returnsSavedPatientResponse() {
        when(patientRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatientResponse response = service.registerPatient(validRegisterRequest());

        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john@test.com");
        assertThat(response.dateOfBirth()).isEqualTo(DOB);
        assertThat(response.phone()).isEqualTo("555-0000");
    }

    @Test
    void register_normalizesEmailToLowercaseBeforeLookupAndSave() {
        RegisterPatientRequest request = new RegisterPatientRequest(
                "John Doe", " John@Test.com ", DOB, "555-0000");
        when(patientRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registerPatient(request);

        verify(patientRepository).findByEmail("john@test.com");
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void register_rejectsDuplicateEmail_onPreCheck() {
        Patient existing = spyPatient(1L, "Existing", "john@test.com", "000");
        when(patientRepository.findByEmail("john@test.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerPatient(validRegisterRequest()))
                .isInstanceOf(DuplicateEmailException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void register_rejectsDuplicateEmail_onSaveTimeRaceCondition() {
        when(patientRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.registerPatient(validRegisterRequest()))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_duplicateEmailCheckIsCaseInsensitive() {
        RegisterPatientRequest request = new RegisterPatientRequest(
                "John Doe", "John@Test.com", DOB, "555-0000");
        Patient existing = spyPatient(1L, "Existing", "john@test.com", "000");
        when(patientRepository.findByEmail("john@test.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerPatient(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // ── list ────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAllPatientsMappedToResponse() {
        Patient p1 = spyPatient(1L, "Alice", "alice@test.com", "111");
        Patient p2 = spyPatient(2L, "Bob", "bob@test.com", "222");
        when(patientRepository.findAll(any(Sort.class))).thenReturn(List.of(p1, p2));

        List<PatientResponse> responses = service.listPatients();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(PatientResponse::name).containsExactly("Alice", "Bob");
    }

    @Test
    void list_returnsEmptyListWhenNoPatients() {
        when(patientRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertThat(service.listPatients()).isEmpty();
    }

    @Test
    void list_requestsIdAscendingSortOrder() {
        when(patientRepository.findAll(any(Sort.class))).thenReturn(List.of());

        service.listPatients();

        verify(patientRepository).findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    // ── get ─────────────────────────────────────────────────────────────────

    @Test
    void get_happyPath_returnsPatientResponse() {
        Patient patient = spyPatient(5L, "John Doe", "john@test.com", "555-0000");
        when(patientRepository.findById(5L)).thenReturn(Optional.of(patient));

        PatientResponse response = service.getPatient(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("John Doe");
    }

    @Test
    void get_rejectsUnknownId() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPatient(99L))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // ── update ──────────────────────────────────────────────────────────────

    @Test
    void update_happyPath_updatesNameEmailPhone() {
        Patient existing = spyPatient(1L, "Old Name", "old@test.com", "000");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatientResponse response = service.updatePatient(1L,
                new UpdatePatientRequest("New Name", "new@test.com", "111"));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("new@test.com");
        assertThat(response.phone()).isEqualTo("111");
        assertThat(response.dateOfBirth()).isEqualTo(DOB);
    }

    @Test
    void update_rejectsUnknownId() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePatient(99L,
                new UpdatePatientRequest("Name", "email@test.com", "000")))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void update_allowsUnchangedEmail_doesNotSelfConflict() {
        Patient existing = spyPatient(1L, "Name", "same@test.com", "000");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.findByEmail("same@test.com")).thenReturn(Optional.of(existing));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> service.updatePatient(1L,
                new UpdatePatientRequest("Name", "same@test.com", "111")));
    }

    @Test
    void update_rejectsEmailAlreadyUsedByAnotherPatient() {
        Patient existing = spyPatient(1L, "Name", "old@test.com", "000");
        Patient other = spyPatient(2L, "Other", "taken@test.com", "222");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updatePatient(1L,
                new UpdatePatientRequest("Name", "taken@test.com", "000")))
                .isInstanceOf(DuplicateEmailException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void update_rejectsDuplicateEmail_onSaveTimeRaceCondition() {
        Patient existing = spyPatient(1L, "Name", "old@test.com", "000");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.updatePatient(1L,
                new UpdatePatientRequest("Name", "new@test.com", "111")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void update_normalizesEmailToLowercaseBeforeLookupAndSave() {
        Patient existing = spyPatient(1L, "Name", "old@test.com", "000");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updatePatient(1L, new UpdatePatientRequest("Name", " New@Test.com ", "111"));

        verify(patientRepository).findByEmail("new@test.com");
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void update_doesNotChangeDateOfBirth() {
        Patient existing = spyPatient(1L, "Name", "old@test.com", "000");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updatePatient(1L, new UpdatePatientRequest("Name", "new@test.com", "111"));

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        assertThat(captor.getValue().getDateOfBirth()).isEqualTo(DOB);
    }
}
