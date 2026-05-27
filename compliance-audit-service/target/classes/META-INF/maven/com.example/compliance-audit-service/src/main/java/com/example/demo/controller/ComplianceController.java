package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ErrorResponseDTO;
import com.example.demo.dto.client_dto.SubjectLookupDTO;
import com.example.demo.dto.compliance_audit.ComplianceLookupDTO;
import com.example.demo.dto.compliance_audit.ComplianceRecordCreateRequestDTO;
import com.example.demo.dto.compliance_audit.ComplianceResponseDTO;
import com.example.demo.model.Enums.ComplianceSubjectType;
import com.example.demo.service.ComplianceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

	private final ComplianceService service;

	// CREATE COMPLIANCE
	@PostMapping
	public ResponseEntity<?> createRecord(@RequestParam Long officerUserId,
			@RequestBody @Valid ComplianceRecordCreateRequestDTO dto) {

		try {
			ComplianceResponseDTO newCompliance = service.recordCompliance(dto, officerUserId);

			return ResponseEntity.status(HttpStatus.CREATED).body(newCompliance);

		} catch (IllegalArgumentException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ErrorResponseDTO(ex.getMessage(), LocalDateTime.now()));

		} catch (IllegalStateException ex) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new ErrorResponseDTO(ex.getMessage(), LocalDateTime.now()));
		}
	}

	// GET BY SUBJECT
	@GetMapping("/subject")
	public ResponseEntity<List<ComplianceResponseDTO>> getBySubject(@RequestParam ComplianceSubjectType subjectType,
			@RequestParam Long subjectId) {

		return ResponseEntity.ok(service.getBySubject(subjectType, subjectId));
	}

	// SUBJECT LOOKUPS
	@GetMapping("/subjects/programs")
	public ResponseEntity<List<SubjectLookupDTO>> getPrograms() {
		return ResponseEntity.ok(service.getProgramSubjects());
	}

	@GetMapping("/subjects/projects")
	public ResponseEntity<List<SubjectLookupDTO>> getProjects() {
		return ResponseEntity.ok(service.getProjectSubjects());
	}

	@GetMapping("/subjects/incentives")
	public ResponseEntity<List<SubjectLookupDTO>> getIncentives() {
		return ResponseEntity.ok(service.getIncentiveSubjects());
	}

	// LOOKUP
	@GetMapping("/lookup")
	public ResponseEntity<List<ComplianceLookupDTO>> getComplianceLookup() {
		return ResponseEntity.ok(service.getComplianceLookup());
	}

	// REPORT METRICS
	@GetMapping("/report-metrics")
	public ResponseEntity<Map<String, Object>> getComplianceReportMetrics() {
		return ResponseEntity.ok(service.getComplianceReportMetrics());
	}
}
