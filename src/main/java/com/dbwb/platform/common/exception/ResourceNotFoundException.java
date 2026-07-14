package com.dbwb.platform.common.exception;

/** Thrown when a requested entity does not exist or is not visible to the caller. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
