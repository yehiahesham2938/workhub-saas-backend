package com.workhub.saasbackend.exception;

public class MissingTenantException extends RuntimeException {

    public MissingTenantException(String message) {
        super(message);
    }
}
