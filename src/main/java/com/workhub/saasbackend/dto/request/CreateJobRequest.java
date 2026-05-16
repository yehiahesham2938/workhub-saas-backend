package com.workhub.saasbackend.dto.request;

import jakarta.validation.constraints.Size;

public class CreateJobRequest {

    @Size(max = 128)
    private String idempotencyKey;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
