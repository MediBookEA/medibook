package com.medibook.controller;

import com.medibook.api.dto.AppointmentResponse;
import com.medibook.api.dto.BookAppointmentRequest;
import com.medibook.api.dto.RescheduleAppointmentRequest;
import com.medibook.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse bookAppointment(@Valid @RequestBody BookAppointmentRequest request) {
        return appointmentService.bookAppointment(request);
    }

    @DeleteMapping("/{id}")
    public AppointmentResponse cancelAppointment(@PathVariable Long id) {
        return appointmentService.cancelAppointment(id);
    }

    @PutMapping("/{id}")
    public AppointmentResponse rescheduleAppointment(@PathVariable Long id,
                                                     @Valid @RequestBody RescheduleAppointmentRequest request) {
        return appointmentService.rescheduleAppointment(id, request);
    }

    @GetMapping(params = {"doctorId", "date"})
    public List<AppointmentResponse> getSchedule(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.getSchedule(doctorId, date);
    }

    @GetMapping(params = {"doctorId", "from"})
    public List<AppointmentResponse> getUpcomingSchedule(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        return appointmentService.getUpcomingSchedule(doctorId, from);
    }
}
