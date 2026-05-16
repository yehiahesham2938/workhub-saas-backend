package com.workhub.saasbackend.service;

import java.util.UUID;

import com.workhub.saasbackend.dto.request.CreateJobRequest;
import com.workhub.saasbackend.dto.response.JobResponse;

public interface JobService {

	JobResponse createJob(CreateJobRequest request);

	JobResponse getJob(UUID id);

	JobResponse retryJob(UUID id);
}
