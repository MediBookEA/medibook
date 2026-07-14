package com.medibook.service;

import com.medibook.api.dto.DoctorResponse;
import com.medibook.api.dto.WorkingHoursResponse;
import com.medibook.domain.Doctor;
import com.medibook.domain.WorkingHours;
import com.medibook.repository.DoctorRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<DoctorResponse> listDoctors() {
        return doctorRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private DoctorResponse toResponse(Doctor doctor) {
        List<WorkingHoursResponse> workingHours = doctor.getWorkingHours().stream()
                .sorted(Comparator.comparing(WorkingHours::getDayOfWeek))
                .map(wh -> new WorkingHoursResponse(wh.getDayOfWeek(), wh.getStartTime(), wh.getEndTime()))
                .toList();
        return new DoctorResponse(doctor.getId(), doctor.getName(), doctor.getSpecialty(), workingHours);
    }
}
