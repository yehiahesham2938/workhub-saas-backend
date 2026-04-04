package com.workhub.saasbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "jobs")
public class Job extends BaseTenantEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private JobStatus status;

	@Column(length = 512)
	private String errorMessage;

	public Job() {
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}

