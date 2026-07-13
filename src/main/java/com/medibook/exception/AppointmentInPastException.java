package com.medibook.exception;

public class AppointmentInPastException extends RuntimeException {
    public AppointmentInPastException() {
        super("Appointment must be in the future");
    }
}
