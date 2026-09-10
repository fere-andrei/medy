package com.example.medy.core.web;

/** Thrown when the caller is authenticated but not allowed to perform this specific operation (403). */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
