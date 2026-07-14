package com.medibook.controller;

import com.medibook.api.dto.DoctorResponse;
import com.medibook.api.dto.WorkingHoursResponse;
import com.medibook.service.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private DoctorService service;

    private DoctorResponse doctorResponse() {
        return new DoctorResponse(1L, "Dr. Smith", "Cardiology",
                List.of(new WorkingHoursResponse(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0))));
    }

    // ── GET /api/v1/doctors ────────────────────────────────────────────────────

    @Test
    void getDoctors_returnsListWith200() throws Exception {
        when(service.listDoctors()).thenReturn(List.of(doctorResponse()));

        mvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dr. Smith"))
                .andExpect(jsonPath("$[0].specialty").value("Cardiology"))
                .andExpect(jsonPath("$[0].workingHours[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    void getDoctors_returnsEmptyListWith200WhenNoDoctors() throws Exception {
        when(service.listDoctors()).thenReturn(List.of());

        mvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
