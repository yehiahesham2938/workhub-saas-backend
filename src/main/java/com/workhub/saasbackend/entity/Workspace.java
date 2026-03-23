package com.workhub.saasbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "workspaces")
public class Workspace extends BaseTenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ownerEmail;
}
