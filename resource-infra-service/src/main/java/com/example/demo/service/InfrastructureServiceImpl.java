package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.client.ProjectClient;
import com.example.demo.dto.InfrastructureCreateRequestDTO;
import com.example.demo.dto.ProjectResponseDTO;
import com.example.demo.dto.UpdateInfrastructureCapacityRequest;
import com.example.demo.exception.InfrastructureNotFoundException;
import com.example.demo.model.Infrastructure;
import com.example.demo.repository.InfrastructureRepository;

import enums.InfrastructureStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfrastructureServiceImpl implements InfrastructureService {

	private final InfrastructureRepository infrastructureRepository;
	private final ProjectClient projectClient;

	@Override
	@Transactional
	public Infrastructure createInfrastructure(InfrastructureCreateRequestDTO request) {
		log.info("Attempting to create infrastructure '{}' for Project ID: {}", request.getInfrastructureName(),
				request.getProjectId());

		ProjectResponseDTO project = fetchProject(request.getProjectId());

		if (!"Approved".equalsIgnoreCase(project.getStatus())) {
			log.warn("Validation failed: Project ID {} status is '{}', expected 'Approved'", request.getProjectId(),
					project.getStatus());
			throw new ValidationException("Infrastructure can only be managed for 'Approved' projects. Current status: "
					+ project.getStatus());
		}

		Infrastructure infrastructure = Infrastructure.builder().projectId(request.getProjectId())
				.projectName(project.getTitle()).infrastructureName(request.getInfrastructureName())
				.type(request.getType()).location(request.getLocation()).capacity(request.getCapacity())
				.utilizedCapacity(0.0).status(InfrastructureStatus.ACTIVE).build();

		Infrastructure savedInfra = infrastructureRepository.save(infrastructure);
		log.info("Infrastructure successfully created with ID: {} and status: {}", savedInfra.getInfraId(),
				savedInfra.getStatus());
		return savedInfra;
	}

	@Override
	@Transactional
	public Infrastructure updateUtilizedCapacity(Long infraId, UpdateInfrastructureCapacityRequest request) {
		log.info("Processing capacity utilization request for Infrastructure ID: {}, Added Capacity: {}", infraId,
				request.getUtilizedCapacity());

		Infrastructure infrastructure = infrastructureRepository.findById(infraId).orElseThrow(() -> {
			log.error("Capacity update failed: Infrastructure ID {} not found", infraId);
			return new InfrastructureNotFoundException("Infrastructure not found");
		});

		Double currentUtilized = infrastructure.getUtilizedCapacity();
		if (currentUtilized == null) {
			currentUtilized = 0.0;
		}

		Double newUtilizedCapacity = currentUtilized + request.getUtilizedCapacity();

		if (newUtilizedCapacity > infrastructure.getCapacity()) {
			log.warn(
					"Utilization rejected: Exceeds maximum limits for Infra ID {}. Requested Total: {}, Max Capacity: {}",
					infraId, newUtilizedCapacity, infrastructure.getCapacity());
			throw new ValidationException(String.format(
					"Cannot allocate capacity. Requested: %.2f. Current Utilized: %.2f. Remaining Available: %.2f.",
					request.getUtilizedCapacity(), currentUtilized, (infrastructure.getCapacity() - currentUtilized)));
		}

		infrastructure.setUtilizedCapacity(newUtilizedCapacity);

		Double utilized = infrastructure.getUtilizedCapacity();

		if (utilized == null || Math.abs(utilized) < 0.0001) {
			infrastructure.setStatus(InfrastructureStatus.INACTIVE);
		} else {
			infrastructure.setStatus(InfrastructureStatus.UNDER_MAINTENANCE);
		}

		Infrastructure updatedInfra = infrastructureRepository.save(infrastructure);
		log.info("Infra ID {} capacity updated to {}. Status set to: {}", infraId, newUtilizedCapacity,
				updatedInfra.getStatus());

		return updatedInfra;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Infrastructure> getAllInfrastructure(Pageable pageable) {
		log.debug("Fetching page {} of infrastructures", pageable.getPageNumber());
		return infrastructureRepository.findAll(pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Infrastructure getInfrastructureById(Long infraId) {
		log.debug("Fetching details for Infrastructure ID: {}", infraId);
		return infrastructureRepository.findById(infraId).orElseThrow(() -> {
			log.error("Fetch failed: Infrastructure ID {} not found", infraId);
			return new InfrastructureNotFoundException("Infrastructure not found");
		});
	}

	@Override
	@Transactional
	public void deleteInfrastructure(Long infraId) {
		log.info("Attempting to delete Infrastructure ID: {}", infraId);

		Infrastructure infrastructure = infrastructureRepository.findById(infraId).orElseThrow(() -> {
			log.error("Deletion failed: Infrastructure ID {} not found", infraId);
			return new InfrastructureNotFoundException("Infrastructure not found");
		});

		infrastructureRepository.delete(infrastructure);
		log.info("Infrastructure ID {} successfully deleted from database", infraId);
	}

	@CircuitBreaker(name = "projectServiceBreaker", fallbackMethod = "fallbackFetchProject")
	private ProjectResponseDTO fetchProject(long projectId) {
		log.info("Calling Project Microservice via Feign for Project ID: {}", projectId);
		ResponseEntity<ProjectResponseDTO> response = projectClient.getProjectById(projectId);
		ProjectResponseDTO project = response.getBody();

		if (project == null || "SERVICE_FAILURE".equals(project.getStatus())) {
			throw new RuntimeException("Target payload missing or returned service failure state");
		}
		return project;
	}

	private ProjectResponseDTO fallbackFetchProject(long projectId, Throwable throwable) {
		log.error("Circuit Breaker Tripped! Project service down or unreachable for ID: {}. Reason: {}", projectId,
				throwable.getMessage());
		throw new InfrastructureNotFoundException(
				"Project Microservice is temporarily down or unreachable. Please try again in a few moments.");
	}

}