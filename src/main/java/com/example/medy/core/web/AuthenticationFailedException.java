package com.example.medy.core.web;

/** Thrown when a supplied credential (login password, current password on a change) doesn't match (401). */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
