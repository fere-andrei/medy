package com.example.medy.core.web;

/** Thrown when a request is well-formed but violates a business rule (400) — distinct from bean-validation failures. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
