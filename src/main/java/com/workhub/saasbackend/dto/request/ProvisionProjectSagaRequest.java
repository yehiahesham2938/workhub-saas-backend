package com.workhub.saasbackend.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class ProvisionProjectSagaRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotEmpty
    private List<String> defaultTaskStatuses;

    private boolean simulateFailure;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDefaultTaskStatuses() {
        return defaultTaskStatuses;
    }

    public void setDefaultTaskStatuses(List<String> defaultTaskStatuses) {
        this.defaultTaskStatuses = defaultTaskStatuses;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
