package com.example.demo.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.clients.UserClient;
import com.example.demo.dto.UserBasicDTO;
import com.example.demo.dto.compliance_audit.AuditCreateRequestDTO;
import com.example.demo.dto.compliance_audit.AuditResponseDTO;
import com.example.demo.exception.ServiceUnavailableException;
import com.example.demo.mapper.AuditMapper;
import com.example.demo.model.Audit;
import com.example.demo.model.ComplianceRecord;
import com.example.demo.model.Enums.AuditStatus;
import com.example.demo.model.Enums.ComplianceAuditStatus;
import com.example.demo.model.Enums.ReportScope;
import com.example.demo.repo.AuditRepository;
import com.example.demo.repo.ComplianceRecordRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditServiceImpl implements AuditService {

	private final AuditRepository auditRepo;
	private final ComplianceRecordRepository complianceRepo;
	private final UserClient userClient;
	private final ComplianceService complianceService;

	// START AUDIT
	@Override
	public AuditResponseDTO startAudit(AuditCreateRequestDTO dto, Long auditorUserId) {
		UserBasicDTO auditor;
		try {

			auditor = fetchAuthService(auditorUserId); // fetchAuthService is the circuit breaker where we are calling
														// userClient.getUserById
		} catch (Exception e) {
			throw new IllegalArgumentException("Auth Service functionality is down, please try again after some time");
		}
		// Validate compliance
		ComplianceRecord compliance = complianceRepo.findById(dto.getComplianceId())
				.orElseThrow(() -> new IllegalArgumentException("Compliance not found"));

		if (compliance.getAuditStatus() == ComplianceAuditStatus.FLAGGED) {
			throw new IllegalStateException("Audit already exists and is in Flagged state for this compliance");
		}

		if (compliance.getAuditStatus() == ComplianceAuditStatus.VERIFIED) {
			throw new IllegalStateException("Compliance already verified. Cannot start new audit");
		}

		// Create audit
		Audit audit = new Audit();
		audit.setComplianceId(dto.getComplianceId());
		audit.setOfficerUserId(auditorUserId);
		audit.setOpenedDate(Instant.now());
		audit.setStatus(AuditStatus.IN_PROGRESS);

		audit.setScope(ReportScope.valueOf(compliance.getSubjectType().name()));

		audit.setCreatedBy(auditor.getUsername());
		audit.setUpdatedBy(auditor.getUsername());

		Audit saved = auditRepo.save(audit);

		// label only for response
		String subjectName = complianceService.getSubjectName(compliance.getSubjectType(), compliance.getSubjectId());

		String label = compliance.getSubjectType() + " - " + subjectName;

		return AuditMapper.toDTO(saved, label);
	}

	@Override
	public AuditResponseDTO closeAudit(Long auditId, AuditCreateRequestDTO dto, Long auditorUserId) {

		Audit audit = auditRepo.findById(auditId).orElseThrow(() -> new IllegalArgumentException("Audit not found"));

		if (audit.getStatus() != AuditStatus.IN_PROGRESS) {
			throw new IllegalStateException("Only IN_PROGRESS audits can be closed");
		}

		UserBasicDTO auditor = fetchAuthService(auditorUserId);

		// update audit
		audit.setFindings(dto.getFindings());
		audit.setSeverity(dto.getSeverity());
		audit.setStatus(AuditStatus.COMPLETED);
		audit.setClosedDate(Instant.now());
		audit.setUpdatedBy(auditor.getUsername());

		// update compliance
		ComplianceRecord compliance = complianceRepo.findById(audit.getComplianceId())
				.orElseThrow(() -> new IllegalArgumentException("Compliance not found"));

		compliance.setAuditStatus(dto.getAuditStatus());

		Audit saved = auditRepo.save(audit);

		return AuditMapper.toDTO(saved);
	}

	// GET AUDITS BY COMPLIANCE
	@Override
	public List<AuditResponseDTO> getByCompliance(Long complianceId) {

		return auditRepo.findByComplianceId(complianceId).stream().map(audit -> {

			ComplianceRecord comp = complianceRepo.findById(audit.getComplianceId()).orElse(null);

			String label = null;

			if (comp != null) {
				String subjectName = complianceService.getSubjectName(comp.getSubjectType(), comp.getSubjectId());

				label = comp.getSubjectType() + " - " + subjectName;
			}

			return AuditMapper.toDTO(audit, label);
		}).toList();
	}

	// GET ALL AUDITS
	@Override
	public List<AuditResponseDTO> getAllAudit() {

		return auditRepo.findAll().stream().map(audit -> {

			ComplianceRecord comp = complianceRepo.findById(audit.getComplianceId()).orElse(null);

			String label = null;

			if (comp != null) {
				String subjectName = complianceService.getSubjectName(comp.getSubjectType(), comp.getSubjectId());

				label = comp.getSubjectType() + " - " + subjectName;
			}

			return AuditMapper.toDTO(audit, label);
		}).toList();
	}

	@Override
	public List<AuditResponseDTO> getByStatus(AuditStatus status) {

		return auditRepo.findByStatus(status).stream().map(audit -> {

			ComplianceRecord comp = complianceRepo.findById(audit.getComplianceId()).orElse(null);

			String label = null;

			if (comp != null) {
				String subjectName = complianceService.getSubjectName(comp.getSubjectType(), comp.getSubjectId());

				label = comp.getSubjectType() + " - " + subjectName;
			}

			return AuditMapper.toDTO(audit, label);
		}).toList();
	}

	// ================= HELPER =================
	// For auth service circuit breaker
	@CircuitBreaker(name = "authService", fallbackMethod = "authFallBack")
	private UserBasicDTO fetchAuthService(Long userId) {
		return userClient.getUserById(userId);
	}

	// Fallback method for auth
	private UserBasicDTO authFallBack(Long userId) {
		log.error("Auth Service unavailable auditor Id {}", userId);
		throw new ServiceUnavailableException("Auth Service unavailable");
	}

}