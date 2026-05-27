package com.example.demo.dto.compliance_audit;

import com.example.demo.model.Enums.ComplianceAuditStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditCreateRequestDTO {

	@NotNull(message = "complianceId is required")
	@Positive(message = "complianceId must be positive")
	private Long complianceId;

	private ComplianceAuditStatus auditStatus;

	// we are keeping it null for starting an audit but using the same DTO for close audit.
	@Min(value = 1, message = "severity must be at least 1")
	@Max(value = 5, message = "severity must be at most 5")
	private Integer severity;

	// we are keeping it null for starting an audit but using the same DTO for close audit.
	@Size(max = 5000, min = 10, message = "findings cannot exceed 5000 characters")
	private String findings;
}