package com.workhub.saasbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspaces")
public class Workspace extends BaseTenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ownerEmail;

    public Workspace() {
    }

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
