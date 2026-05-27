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

import com.example.demo.dto.InfrastructureCreateRequestDTO;
import com.example.demo.dto.UpdateInfrastructureCapacityRequest;
import com.example.demo.model.Infrastructure;
import com.example.demo.service.InfrastructureService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor // Automatically creates constructor for the 'final' service field
public class InfrastructureController {

    private final InfrastructureService infrastructureService;

    @PostMapping
    public ResponseEntity<Infrastructure> createInfrastructure(
            @Valid @RequestBody InfrastructureCreateRequestDTO request) {

        return ResponseEntity.ok(
                infrastructureService.createInfrastructure(request));
    }

    @PutMapping("/{infraId}/capacity")
    public ResponseEntity<Infrastructure> updateUtilizedCapacity(
            @PathVariable Long infraId,
            @Valid @RequestBody UpdateInfrastructureCapacityRequest request) {

        return ResponseEntity.ok(
                infrastructureService.updateUtilizedCapacity(
                        infraId,
                        request));
    }

    @GetMapping
    public ResponseEntity<Page<Infrastructure>> getAllInfrastructure(
            Pageable pageable) {

        return ResponseEntity.ok(
                infrastructureService.getAllInfrastructure(pageable));
    }

    @GetMapping("/{infraId}")
    public ResponseEntity<Infrastructure> getInfrastructureById(
            @PathVariable Long infraId) {

        return ResponseEntity.ok(
                infrastructureService.getInfrastructureById(infraId));
    }

    @DeleteMapping("/{infraId}")
    public ResponseEntity<String> deleteInfrastructure(
            @PathVariable Long infraId) {

        infrastructureService.deleteInfrastructure(infraId);

        return ResponseEntity.ok(
                "Infrastructure deleted successfully");
    }
    
    
}