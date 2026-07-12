package com.medibook.exception;

import java.time.LocalDateTime;

public class DoubleBookingException extends RuntimeException {
    public DoubleBookingException(String name, LocalDateTime startTime) {
        super(name + " already has an appointment at " + startTime);
    }
}
