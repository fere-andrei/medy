package com.example.medy.core.web;

/**
 * Thrown by any module when a requested resource doesn't exist (or isn't
 * visible to the current tenant — the two cases are indistinguishable on
 * purpose, since a tenant-scoped lookup that returns "not found" for another
 * tenant's data is the correct, non-leaking behavior, not an error).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
