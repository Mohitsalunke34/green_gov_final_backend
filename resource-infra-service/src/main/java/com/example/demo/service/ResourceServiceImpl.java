package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.client.ProjectClient;
import com.example.demo.dto.AllocateResourceRequest;
import com.example.demo.dto.ProjectResponseDTO;
import com.example.demo.dto.ResourceCreateRequestDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Resource;
import com.example.demo.repository.ResourceRepository;

import enums.ResourceStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ProjectClient projectClient;

    @Override
    @Transactional
    public Resource createResource(ResourceCreateRequestDTO request) {
        log.info("Attempting to create resource '{}' for Project ID: {}", request.getResourceName(), request.getProjectId());

        ProjectResponseDTO project = fetchProject(request.getProjectId());

        if (!"Approved".equalsIgnoreCase(project.getStatus())) {
            log.warn("Validation failed: Project ID {} status is '{}', expected 'Approved'", request.getProjectId(), project.getStatus());
            throw new ValidationException("Resources can only be allocated to 'Approved' projects. Current status: " + project.getStatus());
        }

        Resource resource = Resource.builder()
                .projectId(request.getProjectId())
                .projectName(project.getTitle()) 
                .resourceName(request.getResourceName())
                .type(request.getType())
                .totalQuantity(request.getTotalQuantity())
                .availableQuantity(request.getTotalQuantity())
                .status(ResourceStatus.AVAILABLE)
                .build();

        Resource savedResource = resourceRepository.save(resource);
        log.info("Resource successfully created with ID: {} and status: {}", savedResource.getResourceId(), savedResource.getStatus());
        return savedResource;
    }

    @Override
    @Transactional
    public Resource allocateResource(Long resourceId, AllocateResourceRequest request) {
        log.info("Processing allocation request for Resource ID: {}, Quantity: {}", resourceId, request.getAllocationQuantity());

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> {
                    log.error("Allocation failed: Resource ID {} not found", resourceId);
                    return new ResourceNotFoundException("Resource not found");
                });

        if (request.getAllocationQuantity() > resource.getAvailableQuantity()) {
            log.warn("Allocation rejected: Insufficient quantity for Resource ID {}. Requested: {}, Available: {}", 
                    resourceId, request.getAllocationQuantity(), resource.getAvailableQuantity());
            throw new ValidationException("Insufficient quantity available. Requested: " 
                    + request.getAllocationQuantity() + ", Available: " + resource.getAvailableQuantity());
        }

        Double remainingQuantity = resource.getAvailableQuantity() - request.getAllocationQuantity();
        resource.setAvailableQuantity(remainingQuantity);

        
        if (remainingQuantity == 0) {
            resource.setStatus(ResourceStatus.EXHAUSTED);
        } else {
            resource.setStatus(ResourceStatus.ALLOCATED);
        }

        Resource updatedResource = resourceRepository.save(resource);
        log.info("Resource ID {} updated. Remaining Quantity: {}, Status set to: {}", 
                resourceId, remainingQuantity, updatedResource.getStatus());

        return updatedResource;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Resource> getAllResources(Pageable pageable) {
        log.debug("Fetching page {} of resources", pageable.getPageNumber());
        return resourceRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getResourceById(Long resourceId) {
        log.debug("Fetching resource details for ID: {}", resourceId);
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> {
                    log.error("Fetch failed: Resource ID {} not found", resourceId);
                    return new ResourceNotFoundException("Resource not found");
                });
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId) {
        log.info("Attempting to delete Resource ID: {}", resourceId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> {
                    log.error("Deletion failed: Resource ID {} not found", resourceId);
                    return new ResourceNotFoundException("Resource not found");
                });

        if (resource.getStatus() == ResourceStatus.ALLOCATED) {
            log.warn("Deletion rejected: Resource ID {} is currently status ALLOCATED", resourceId);
            throw new ValidationException("Cannot delete resource with ID " + resourceId + " because its current status is ALLOCATED.");
        }

        resourceRepository.delete(resource);
        log.info("Resource ID {} successfully deleted from database", resourceId);
    }
    
    
    @CircuitBreaker(name = "projectServiceBreaker", fallbackMethod = "fallbackFetchProject")
    private ProjectResponseDTO fetchProject(long projectId) {
        log.info("Calling Project Microservice for Project ID: {}", projectId);
        ResponseEntity<ProjectResponseDTO> response = projectClient.getProjectById(projectId);
        ProjectResponseDTO project = response.getBody();

        if (project == null || "SERVICE_FAILURE".equals(project.getStatus())) {
            throw new RuntimeException("Target payload missing or returned service failure state");
        }
        return project;
    }

    
    private ProjectResponseDTO fallbackFetchProject(long projectId, Throwable throwable) {
        log.error("Circuit Breaker Tripped/Active! Project service un-contactable for ID: {}. Error Reason: {}", 
                projectId, throwable.getMessage());
        
        // Throw a clean exception handled by your GlobalExceptionHandler to notify users cleanly
        throw new ResourceNotFoundException("Project Microservice is temporarily down or unreachable. Please try again in a few moments.");
    }
    
}