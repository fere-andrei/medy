package com.example.medy.core.web;

/** Thrown when a request would violate a uniqueness or scheduling constraint (409). */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
