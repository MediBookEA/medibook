package com.medibook.exception;

public class OutsideWorkingHoursException extends RuntimeException {
    public OutsideWorkingHoursException(String doctorName) {
        super("Appointment falls outside " + doctorName + "'s working hours");
    }
}
