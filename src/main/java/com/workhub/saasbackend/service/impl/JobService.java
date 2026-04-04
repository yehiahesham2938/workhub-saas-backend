package com.workhub.saasbackend.service;

import java.util.UUID;

import com.workhub.saasbackend.dto.response.JobResponse;

public interface JobService {

	JobResponse createJob();

	JobResponse getJob(UUID id);
}

