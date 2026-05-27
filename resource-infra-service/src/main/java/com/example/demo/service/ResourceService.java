package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AllocateResourceRequest;
import com.example.demo.dto.ResourceCreateRequestDTO;
import com.example.demo.model.Resource;


@Service
public interface ResourceService {
    
    Resource createResource(ResourceCreateRequestDTO request);

    Resource allocateResource(Long resourceId, AllocateResourceRequest request);

    Page<Resource> getAllResources(Pageable pageable);

    Resource getResourceById(Long resourceId);

    void deleteResource(Long resourceId);
    
    
    
}