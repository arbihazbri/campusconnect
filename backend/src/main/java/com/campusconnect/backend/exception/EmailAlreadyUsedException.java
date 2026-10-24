package com.campusconnect.backend.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Cet email est déjà utilisé");
    }
}