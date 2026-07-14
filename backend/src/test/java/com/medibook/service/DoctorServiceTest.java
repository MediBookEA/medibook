package com.medibook.service;

import com.medibook.api.dto.DoctorResponse;
import com.medibook.api.dto.WorkingHoursResponse;
import com.medibook.domain.Doctor;
import com.medibook.domain.WorkingHours;
import com.medibook.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private Doctor doctor1;
    @Mock private Doctor doctor2;

    private DoctorService service;

    @BeforeEach
    void setUp() {
        service = new DoctorService(doctorRepository);
        lenient().when(doctor1.getId()).thenReturn(1L);
        lenient().when(doctor1.getName()).thenReturn("Dr. Smith");
        lenient().when(doctor1.getSpecialty()).thenReturn("Cardiology");
        lenient().when(doctor2.getId()).thenReturn(2L);
        lenient().when(doctor2.getName()).thenReturn("Dr. Jones");
        lenient().when(doctor2.getSpecialty()).thenReturn("Dermatology");
    }

    // ── list ────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAllDoctorsMappedToResponse() {
        when(doctor1.getWorkingHours()).thenReturn(List.of());
        when(doctor2.getWorkingHours()).thenReturn(List.of());
        when(doctorRepository.findAll(any(Sort.class))).thenReturn(List.of(doctor1, doctor2));

        List<DoctorResponse> responses = service.listDoctors();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(DoctorResponse::name).containsExactly("Dr. Smith", "Dr. Jones");
        assertThat(responses).extracting(DoctorResponse::specialty).containsExactly("Cardiology", "Dermatology");
    }

    @Test
    void list_returnsEmptyListWhenNoDoctors() {
        when(doctorRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertThat(service.listDoctors()).isEmpty();
    }

    @Test
    void list_requestsIdAscendingSortOrder() {
        when(doctorRepository.findAll(any(Sort.class))).thenReturn(List.of());

        service.listDoctors();

        verify(doctorRepository).findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void list_mapsWorkingHoursSortedByDayOfWeek() {
        WorkingHours friday = new WorkingHours(doctor1, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        WorkingHours monday = new WorkingHours(doctor1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        when(doctor1.getWorkingHours()).thenReturn(List.of(friday, monday));

        when(doctorRepository.findAll(any(Sort.class))).thenReturn(List.of(doctor1));

        List<DoctorResponse> responses = service.listDoctors();

        assertThat(responses.get(0).workingHours())
                .extracting(WorkingHoursResponse::dayOfWeek)
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void list_doctorWithNoWorkingHours_returnsEmptyWorkingHoursList() {
        when(doctor1.getWorkingHours()).thenReturn(List.of());
        when(doctorRepository.findAll(any(Sort.class))).thenReturn(List.of(doctor1));

        List<DoctorResponse> responses = service.listDoctors();

        assertThat(responses.get(0).workingHours()).isEmpty();
    }
}
