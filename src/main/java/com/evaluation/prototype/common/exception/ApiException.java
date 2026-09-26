package com.evaluation.prototype.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

public class ApiException extends RuntimeException {

    @NonNull
    private final HttpStatus status;

    public ApiException(@NonNull HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    @NonNull
    public HttpStatus getStatus() {
        return status;
    }
}
