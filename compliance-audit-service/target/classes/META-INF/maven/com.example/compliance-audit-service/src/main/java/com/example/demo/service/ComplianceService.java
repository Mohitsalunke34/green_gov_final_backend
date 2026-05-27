package com.example.demo.service;

import java.util.List;
import java.util.Map;

import com.example.demo.dto.client_dto.SubjectLookupDTO;
import com.example.demo.dto.compliance_audit.ComplianceLookupDTO;
import com.example.demo.dto.compliance_audit.ComplianceRecordCreateRequestDTO;
import com.example.demo.dto.compliance_audit.ComplianceResponseDTO;
import com.example.demo.model.Enums.ComplianceSubjectType;

public interface ComplianceService {

	ComplianceResponseDTO recordCompliance(ComplianceRecordCreateRequestDTO dto, Long officerUserId);

	List<ComplianceResponseDTO> getBySubject(ComplianceSubjectType subjectType, Long subjectId);

	// To get client call from Energy program
	List<SubjectLookupDTO> getProgramSubjects();

	// To get client call from Energy projects
	List<SubjectLookupDTO> getProjectSubjects();

	// To get client call from incentives
	List<SubjectLookupDTO> getIncentiveSubjects();

	String getSubjectName(ComplianceSubjectType type, Long subjectId);

	List<ComplianceLookupDTO> getComplianceLookup();

	Map<String, Object> getComplianceReportMetrics();
}
