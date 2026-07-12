package com.medibook.config;

import com.medibook.api.dto.ErrorResponse;
import com.medibook.exception.AppointmentInPastException;
import com.medibook.exception.AppointmentNotFoundException;
import com.medibook.exception.DoctorNotFoundException;
import com.medibook.exception.DoubleBookingException;
import com.medibook.exception.DuplicateEmailException;
import com.medibook.exception.InvalidStartTimeException;
import com.medibook.exception.OutsideWorkingHoursException;
import com.medibook.exception.PatientNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DoubleBookingException.class)
    ResponseEntity<ErrorResponse> handleDoubleBooking(DoubleBookingException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "DOUBLE_BOOKING", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), req);
    }

    @ExceptionHandler(OutsideWorkingHoursException.class)
    ResponseEntity<ErrorResponse> handleOutsideWorkingHours(OutsideWorkingHoursException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "OUTSIDE_WORKING_HOURS", ex.getMessage(), req);
    }

    @ExceptionHandler(AppointmentInPastException.class)
    ResponseEntity<ErrorResponse> handleInPast(AppointmentInPastException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "APPOINTMENT_IN_PAST", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidStartTimeException.class)
    ResponseEntity<ErrorResponse> handleInvalidStart(InvalidStartTimeException ex, HttpServletRequest req) {
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_START_TIME", ex.getMessage(), req);
    }

    @ExceptionHandler({PatientNotFoundException.class, DoctorNotFoundException.class,
            AppointmentNotFoundException.class})
    ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String errorCode,
                                                 String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(LocalDateTime.now(), status.value(), errorCode, message, req.getRequestURI()));
    }
}
