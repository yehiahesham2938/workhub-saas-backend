package com.workhub.saasbackend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workhub.saasbackend.dto.request.CreateJobRequest;
import com.workhub.saasbackend.dto.response.JobResponse;
import com.workhub.saasbackend.service.JobService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/jobs")
@Validated
public class JobController {

	private final JobService jobService;

	public JobController(JobService jobService) {
		this.jobService = jobService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
	public JobResponse createJob(@Valid @RequestBody(required = false) CreateJobRequest request) {
		return jobService.createJob(request != null ? request : new CreateJobRequest());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
	public JobResponse getJob(@PathVariable UUID id) {
		return jobService.getJob(id);
	}
}

