package com.workhub.saasbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    private String name;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Owner email must be valid")
    private String ownerEmail;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }
}
