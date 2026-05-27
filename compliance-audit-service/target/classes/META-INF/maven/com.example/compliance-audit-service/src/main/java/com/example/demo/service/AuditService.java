package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.compliance_audit.AuditCreateRequestDTO;
import com.example.demo.dto.compliance_audit.AuditResponseDTO;
import com.example.demo.model.Enums.AuditStatus;

public interface AuditService {

	AuditResponseDTO startAudit(AuditCreateRequestDTO dto, Long auditorUserId);

	List<AuditResponseDTO> getByCompliance(Long complianceId);

	List<AuditResponseDTO> getByStatus(AuditStatus status);

	List<AuditResponseDTO> getAllAudit();

	AuditResponseDTO closeAudit(Long auditId, AuditCreateRequestDTO dto, Long auditorUserId);

}