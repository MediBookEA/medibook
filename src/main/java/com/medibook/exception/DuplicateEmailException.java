package com.medibook.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Patient with email " + email + " already exists");
    }
}
