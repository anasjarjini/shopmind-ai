package com.shopmind.backend.exception;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("Un compte avec l'email '" + email + "' existe déjà.");
    }
}
