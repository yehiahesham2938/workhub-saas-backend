package com.workhub.saasbackend.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects",
       uniqueConstraints = {
               @jakarta.persistence.UniqueConstraint(name = "uk_projects_tenant_name", columnNames = {"tenant_id", "name"})
       })
public class Project extends BaseTenantEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    public Project() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
