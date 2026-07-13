package com.medibook.exception;

public class InvalidStartTimeException extends RuntimeException {
    public InvalidStartTimeException() {
        super("Appointment must start on the hour or half-hour");
    }
}
