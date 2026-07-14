package com.medibook.config;

import com.medibook.exception.AppointmentInPastException;
import com.medibook.exception.AppointmentNotFoundException;
import com.medibook.exception.DoctorNotFoundException;
import com.medibook.exception.DoubleBookingException;
import com.medibook.exception.DuplicateEmailException;
import com.medibook.exception.InvalidStartTimeException;
import com.medibook.exception.OutsideWorkingHoursException;
import com.medibook.exception.PatientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class StubController {
        @GetMapping("/test/double-booking")
        void doubleBooking() {
            throw new DoubleBookingException("Dr. Test", LocalDateTime.of(2027, 1, 4, 9, 0));
        }

        @GetMapping("/test/duplicate-email")
        void duplicateEmail() { throw new DuplicateEmailException("john@test.com"); }

        @GetMapping("/test/outside-hours")
        void outsideHours() { throw new OutsideWorkingHoursException("Dr. Test"); }

        @GetMapping("/test/in-past")
        void inPast() { throw new AppointmentInPastException(); }

        @GetMapping("/test/invalid-start")
        void invalidStart() { throw new InvalidStartTimeException(); }

        @GetMapping("/test/patient-not-found")
        void patientNotFound() { throw new PatientNotFoundException(1L); }

        @GetMapping("/test/doctor-not-found")
        void doctorNotFound() { throw new DoctorNotFoundException(2L); }

        @GetMapping("/test/appointment-not-found")
        void appointmentNotFound() { throw new AppointmentNotFoundException(3L); }
    }

    // ── 409 ──────────────────────────────────────────────────────────────────

    @Test
    void doubleBookingException_returns409WithFullErrorBody() throws Exception {
        mvc.perform(get("/test/double-booking"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DOUBLE_BOOKING"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/test/double-booking"));
    }

    @Test
    void duplicateEmailException_returns409WithFullErrorBody() throws Exception {
        mvc.perform(get("/test/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/test/duplicate-email"));
    }

    // ── 422 ──────────────────────────────────────────────────────────────────

    @Test
    void outsideWorkingHoursException_returns422() throws Exception {
        mvc.perform(get("/test/outside-hours"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("OUTSIDE_WORKING_HOURS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void appointmentInPastException_returns422() throws Exception {
        mvc.perform(get("/test/in-past"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("APPOINTMENT_IN_PAST"));
    }

    @Test
    void invalidStartTimeException_returns422() throws Exception {
        mvc.perform(get("/test/invalid-start"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("INVALID_START_TIME"));
    }

    // ── 404 ──────────────────────────────────────────────────────────────────

    @Test
    void patientNotFoundException_returns404() throws Exception {
        mvc.perform(get("/test/patient-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void doctorNotFoundException_returns404() throws Exception {
        mvc.perform(get("/test/doctor-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void appointmentNotFoundException_returns404() throws Exception {
        mvc.perform(get("/test/appointment-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
