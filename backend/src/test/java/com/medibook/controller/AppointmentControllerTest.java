package com.medibook.controller;

import tools.jackson.databind.ObjectMapper;
import com.medibook.api.dto.AppointmentResponse;
import com.medibook.api.dto.BookAppointmentRequest;
import com.medibook.api.dto.RescheduleAppointmentRequest;
import com.medibook.exception.AppointmentInPastException;
import com.medibook.exception.AppointmentNotFoundException;
import com.medibook.exception.DoctorNotFoundException;
import com.medibook.exception.DoubleBookingException;
import com.medibook.exception.OutsideWorkingHoursException;
import com.medibook.exception.PatientNotFoundException;
import com.medibook.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AppointmentService service;

    private static final LocalDateTime FUTURE_MONDAY_9AM = LocalDateTime.of(2027, 1, 4, 9, 0);

    private AppointmentResponse bookedResponse() {
        return new AppointmentResponse(1L, 10L, "John Doe", 20L, "Dr. Test",
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), "BOOKED");
    }

    private AppointmentResponse cancelledResponse() {
        return new AppointmentResponse(1L, 10L, "John Doe", 20L, "Dr. Test",
                FUTURE_MONDAY_9AM, FUTURE_MONDAY_9AM.plusMinutes(30), "CANCELLED");
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ── POST /api/v1/appointments ─────────────────────────────────────────────

    @Test
    void postAppointment_validRequest_returns201AndBody() throws Exception {
        when(service.bookAppointment(any())).thenReturn(bookedResponse());

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.patientName").value("John Doe"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Test"));
    }

    @Test
    void postAppointment_missingPatientId_returns400() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(null, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postAppointment_missingDoctorId_returns400() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, null, FUTURE_MONDAY_9AM))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postAppointment_missingStartTime_returns400() throws Exception {
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void postAppointment_serviceThrowsDoubleBooking_returns409() throws Exception {
        when(service.bookAppointment(any()))
                .thenThrow(new DoubleBookingException("Dr. Test", FUTURE_MONDAY_9AM));

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DOUBLE_BOOKING"));
    }

    @Test
    void postAppointment_serviceThrowsOutsideWorkingHours_returns422() throws Exception {
        when(service.bookAppointment(any()))
                .thenThrow(new OutsideWorkingHoursException("Dr. Test"));

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("OUTSIDE_WORKING_HOURS"));
    }

    @Test
    void postAppointment_serviceThrowsInPast_returns422() throws Exception {
        when(service.bookAppointment(any()))
                .thenThrow(new AppointmentInPastException());

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("APPOINTMENT_IN_PAST"));
    }

    @Test
    void postAppointment_serviceThrowsPatientNotFound_returns404() throws Exception {
        when(service.bookAppointment(any())).thenThrow(new PatientNotFoundException(10L));

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void postAppointment_serviceThrowsDoctorNotFound_returns404() throws Exception {
        when(service.bookAppointment(any())).thenThrow(new DoctorNotFoundException(20L));

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void postAppointment_errorBodyContainsAllRequiredFields() throws Exception {
        when(service.bookAppointment(any()))
                .thenThrow(new DoubleBookingException("Dr. Test", FUTURE_MONDAY_9AM));

        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookAppointmentRequest(10L, 20L, FUTURE_MONDAY_9AM))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DOUBLE_BOOKING"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/appointments"));
    }

    // ── DELETE /api/v1/appointments/{id} ──────────────────────────────────────

    @Test
    void deleteAppointment_existingId_returns200WithCancelledStatus() throws Exception {
        when(service.cancelAppointment(1L)).thenReturn(cancelledResponse());

        mvc.perform(delete("/api/v1/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void deleteAppointment_unknownId_returns404() throws Exception {
        when(service.cancelAppointment(99L)).thenThrow(new AppointmentNotFoundException(99L));

        mvc.perform(delete("/api/v1/appointments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ── PUT /api/v1/appointments/{id} ─────────────────────────────────────────

    @Test
    void putAppointment_validRequest_returns200() throws Exception {
        LocalDateTime newSlot = LocalDateTime.of(2027, 1, 4, 10, 0);
        AppointmentResponse rescheduled = new AppointmentResponse(1L, 10L, "John Doe", 20L, "Dr. Test",
                newSlot, newSlot.plusMinutes(30), "BOOKED");
        when(service.rescheduleAppointment(eq(1L), any())).thenReturn(rescheduled);

        mvc.perform(put("/api/v1/appointments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RescheduleAppointmentRequest(newSlot))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("2027-01-04T10:00:00"));
    }

    @Test
    void putAppointment_missingStartTime_returns400() throws Exception {
        mvc.perform(put("/api/v1/appointments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RescheduleAppointmentRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void putAppointment_serviceThrowsDoubleBooking_returns409() throws Exception {
        when(service.rescheduleAppointment(eq(1L), any()))
                .thenThrow(new DoubleBookingException("Dr. Test", FUTURE_MONDAY_9AM));

        mvc.perform(put("/api/v1/appointments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RescheduleAppointmentRequest(FUTURE_MONDAY_9AM))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DOUBLE_BOOKING"));
    }

    // ── GET /api/v1/appointments?doctorId=&date= ──────────────────────────────

    @Test
    void getAppointments_validParams_returns200WithList() throws Exception {
        LocalDate date = LocalDate.of(2027, 1, 4);
        when(service.getSchedule(20L, date)).thenReturn(List.of(bookedResponse()));

        mvc.perform(get("/api/v1/appointments")
                        .param("doctorId", "20")
                        .param("date", "2027-01-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("BOOKED"));
    }

    @Test
    void getAppointments_serviceThrowsDoctorNotFound_returns404() throws Exception {
        when(service.getSchedule(eq(99L), any())).thenThrow(new DoctorNotFoundException(99L));

        mvc.perform(get("/api/v1/appointments")
                        .param("doctorId", "99")
                        .param("date", "2027-01-04"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void getAppointments_emptySchedule_returns200EmptyArray() throws Exception {
        when(service.getSchedule(any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/appointments")
                        .param("doctorId", "20")
                        .param("date", "2027-01-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
