package com.workhub.saasbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkspaceRequest {

    @NotBlank(message = "Workspace name is required")
    private String name;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Owner email must be valid")
    private String ownerEmail;
}
