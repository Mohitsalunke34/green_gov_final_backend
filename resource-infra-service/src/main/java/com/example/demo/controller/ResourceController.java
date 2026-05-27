package com.example.demo.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AllocateResourceRequest;
import com.example.demo.dto.ResourceCreateRequestDTO;
import com.example.demo.model.Resource;
import com.example.demo.service.ResourceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<Resource> createResource(
            @Valid @RequestBody ResourceCreateRequestDTO request) {

        return ResponseEntity.ok(
                resourceService.createResource(request));
    }

    @PutMapping("/{resourceId}/allocate")
    public ResponseEntity<Resource> allocateResource(
            @PathVariable Long resourceId,
            @Valid @RequestBody AllocateResourceRequest request) {

        return ResponseEntity.ok(
                resourceService.allocateResource(resourceId, request));
    }

    @GetMapping
    public ResponseEntity<Page<Resource>> getAllResources(
            Pageable pageable) {

        return ResponseEntity.ok(
                resourceService.getAllResources(pageable));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<Resource> getResourceById(
            @PathVariable Long resourceId) {

        return ResponseEntity.ok(
                resourceService.getResourceById(resourceId));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<String> deleteResource(
            @PathVariable Long resourceId) {

        resourceService.deleteResource(resourceId);

        return ResponseEntity.ok("Resource deleted successfully");
    }
    
}