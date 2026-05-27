package com.example.demo.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.clients.EnergyProgramClient;
import com.example.demo.clients.IncentiveClient;
import com.example.demo.clients.NotificationClient;
import com.example.demo.clients.SustainabilityProjectClient;
import com.example.demo.clients.UserClient;
import com.example.demo.dto.NotificationRequestDTO;
import com.example.demo.dto.UserBasicDTO;
import com.example.demo.dto.client_dto.SubjectLookupDTO;
import com.example.demo.dto.compliance_audit.ComplianceLookupDTO;
import com.example.demo.dto.compliance_audit.ComplianceRecordCreateRequestDTO;
import com.example.demo.dto.compliance_audit.ComplianceResponseDTO;
import com.example.demo.exception.ServiceUnavailableException;
import com.example.demo.mapper.ComplianceMapper;
import com.example.demo.model.ComplianceRecord;
import com.example.demo.model.Enums.ComplianceAuditStatus;
import com.example.demo.model.Enums.ComplianceResult;
import com.example.demo.model.Enums.ComplianceSubjectType;
import com.example.demo.repo.ComplianceRecordRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ComplianceServiceImpl implements ComplianceService {

	private final ComplianceRecordRepository complianceRepo;
	private final UserClient userClient;
	private final SustainabilityProjectClient sustainabilityClient;
	private final EnergyProgramClient programClient;
	private final IncentiveClient incentiveClient;
	private final NotificationClient notificationClient;

	// REMOTE CALLS WITH FeignClient
	// ---- AUTH SERVICE
	@CircuitBreaker(name = "authService", fallbackMethod = "authFallback")
	public UserBasicDTO fetchOfficer(Long officerUserId) {
		return userClient.getUserById(officerUserId);
	}

	private UserBasicDTO authFallback(Long officerUserId, Throwable ex) {
		log.error("Auth service unavailable | officerId={}", officerUserId, ex);
		throw new IllegalStateException("Authentication service unavailable");
	}

	// ---- SUBJECT VALIDATION based on subject Id
	@CircuitBreaker(name = "subjectValidationService", fallbackMethod = "subjectFallback")
	public void validateSubject(ComplianceSubjectType type, Long subjectId) {
		try {
			switch (type) {
			// assertExists helper function -> Does something exist? If NOT → throw error
			case PROJECT -> assertExists(sustainabilityClient.projectExists(subjectId), "Project not found");

			case PROGRAM -> assertExists(programClient.programExists(subjectId), "Program not found");

			case INCENTIVE -> assertExists(incentiveClient.incentiveExists(subjectId), "Incentive not found");
			}
		} catch (feign.FeignException ex) {
			throw new ServiceUnavailableException("Dependent service for " + type + " is currently unavailable");
		}
	}

	private void subjectFallback(ComplianceSubjectType type, Long subjectId, Throwable ex) {

		log.error("Subject validation service unavailable | type={} subjectId={}", type, subjectId, ex);

		throw new ServiceUnavailableException("Subject validation service is currently unavailable");
	}

	// Create Compliance
	@Override
	public ComplianceResponseDTO recordCompliance(ComplianceRecordCreateRequestDTO dto, Long complianceOfficerUserId) {

		log.info("Recording compliance | subjectType={} | subjectId={}", dto.getSubjectType(), dto.getSubjectId());

		UserBasicDTO officer = fetchOfficer(complianceOfficerUserId);

		// 1. Validate subject exists
		validateSubject(dto.getSubjectType(), dto.getSubjectId());

		// 2. VALIDATE business rule FIRST (IMPORTANT)
		// New compliance report can be created on the same compliance only if it is
		// flagged.
		boolean exists = complianceRepo.existsBySubjectTypeAndSubjectIdAndAuditStatusIn(dto.getSubjectType(),
				dto.getSubjectId(), List.of(ComplianceAuditStatus.PENDING, ComplianceAuditStatus.VERIFIED));

		if (exists) {
			throw new IllegalStateException("Active compliance already exists for this subject");
		}

		// 3. Now create record
		ComplianceRecord record = new ComplianceRecord();
		record.setSubjectType(dto.getSubjectType());
		record.setSubjectId(dto.getSubjectId());
		record.setComplianceManagerUserId(complianceOfficerUserId);

		// IMPORTANT lifecycle
		record.setAuditStatus(ComplianceAuditStatus.PENDING);

		record.setResult(dto.getResult());
		record.setNotes(dto.getNotes());
		record.setEvidenceURL(dto.getEvidenceURL());

		record.setCreatedBy(officer.getUsername());
		record.setUpdatedBy(officer.getUsername());
		record.setRecordedDate(Instant.now());

		ComplianceRecord saved = complianceRepo.save(record);

		// notification
		notifyCompliance("Compliance recorded for " + dto.getSubjectType() + " ID " + dto.getSubjectId()
				+ " with result " + dto.getResult(), "COMPLIANCE", saved.getId(), complianceOfficerUserId);

		return ComplianceMapper.toDTO(saved);
	}

	// getBySubjectType and subjectId from Compliance repo
	// Used in frontend filter search of Compliances as per subject type and Subject
	// ID.
	@Override
	public List<ComplianceResponseDTO> getBySubject(ComplianceSubjectType subjectType, Long subjectId) {

		return complianceRepo.findBySubjectTypeAndSubjectId(subjectType, subjectId).stream()
				.map(ComplianceMapper::toDTO).toList();
	}

	// Program Client call with circuit braker
	@Override
	@CircuitBreaker(name = "subjectLookupService", fallbackMethod = "programSubjectsFallback")
	public List<SubjectLookupDTO> getProgramSubjects() {
		return programClient.getProgramSubjects();
	}

	// fallback method for program
	private List<SubjectLookupDTO> programSubjectsFallback(Throwable ex) {
		log.error("Program subject lookup failed", ex);
		throw new ServiceUnavailableException("Unable to fetch program list at the moment");
	}

	// Project Client call with circuit braker
	@Override
	@CircuitBreaker(name = "subjectLookupService", fallbackMethod = "projectSubjectsFallback")
	public List<SubjectLookupDTO> getProjectSubjects() {
		return sustainabilityClient.getProjectSubjects();
	}

	private List<SubjectLookupDTO> projectSubjectsFallback(Throwable ex) {
		log.error("Project subject lookup failed", ex);
		throw new ServiceUnavailableException("Unable to fetch project list at the moment");
	}

	// Incentive Client call with circuit braker
	@Override
	@CircuitBreaker(name = "subjectLookupService", fallbackMethod = "incentiveSubjectsFallback")
	public List<SubjectLookupDTO> getIncentiveSubjects() {
		return incentiveClient.getIncentiveSubjects();
	}

	private List<SubjectLookupDTO> incentiveSubjectsFallback(Throwable ex) {
		log.error("Incentive subject lookup failed", ex);
		throw new ServiceUnavailableException("Unable to fetch incentive list at the moment");
	}

	// LookUp for Audit
	// compliance lookup
	@Override
	public List<ComplianceLookupDTO> getComplianceLookup() {

		return complianceRepo.findAll().stream().map(c -> {

			String subjectName = getSubjectName(c.getSubjectType(), c.getSubjectId());

			String label = c.getSubjectType() + " - " + subjectName;

			return new ComplianceLookupDTO(c.getId(), label);

		}).toList();
	}

	@Override
	public String getSubjectName(ComplianceSubjectType type, Long subjectId) {

		try {
			return switch (type) {

			case PROJECT -> sustainabilityClient.getProjectSubjects().stream().filter(p -> p.getId().equals(subjectId))
					.map(p -> p.getName()).findFirst().orElse("Unknown Project");

			case PROGRAM -> programClient.getProgramSubjects().stream().filter(p -> p.getId().equals(subjectId))
					.map(SubjectLookupDTO::getName).findFirst() // findFirst() return optional
					.orElse("Unknown Program");

			case INCENTIVE -> incentiveClient.getIncentiveSubjects().stream().filter(i -> i.getId().equals(subjectId))
					.map(SubjectLookupDTO::getName).findFirst().orElse("Unknown Incentive");
			};

		} catch (Exception ex) {
			return type + " (ID: " + subjectId + ")";
		}
	}

	@Override
	public Map<String, Object> getComplianceReportMetrics() {

		long total = complianceRepo.count();
		long passed = complianceRepo.countByResult(ComplianceResult.PASS);
		long failed = complianceRepo.countByResult(ComplianceResult.FAIL);

		Map<String, Object> response = new HashMap<>();
		response.put("totalAudits", (int) total);
		response.put("compliant", (int) passed);
		response.put("nonCompliant", (int) failed);

		return response;
	}

	// NOTIFICATION SERVICE Circuit breaker
	@CircuitBreaker(name = "notificationService", fallbackMethod = "notificationFallback")
	@Retry(name = "notificationService")
	private void notifyCompliance(String message, String category, Long entityId, Long userId) {

		NotificationRequestDTO request = NotificationRequestDTO.builder().userId(userId).message(message)
				.category(category).entityId(entityId).sendEmail(false).email("admin@greengov.com").build();

		notificationClient.createNotification(request);
		log.info("Compliance notification sent | entityId={}", entityId);
	}

	private void notificationFallback(String message, String category, Long entityId, Throwable ex) {

		log.warn("Notification skipped (service down) | entityId={}", entityId, ex);
	}

	// HELPERS
	private void assertExists(Boolean exists, String message) {
		if (!Boolean.TRUE.equals(exists)) {
			throw new IllegalArgumentException(message);
		}
	}
}
