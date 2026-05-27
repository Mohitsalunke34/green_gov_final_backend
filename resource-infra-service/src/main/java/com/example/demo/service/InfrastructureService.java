package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.dto.InfrastructureCreateRequestDTO;
import com.example.demo.dto.UpdateInfrastructureCapacityRequest;
import com.example.demo.model.Infrastructure;

@Service
public interface InfrastructureService {

    Infrastructure createInfrastructure(
            InfrastructureCreateRequestDTO request);

    Infrastructure updateUtilizedCapacity(
            Long infraId,
            UpdateInfrastructureCapacityRequest request);

    Page<Infrastructure> getAllInfrastructure(Pageable pageable);

    Infrastructure getInfrastructureById(Long infraId);

    void deleteInfrastructure(Long infraId);

    
    
}