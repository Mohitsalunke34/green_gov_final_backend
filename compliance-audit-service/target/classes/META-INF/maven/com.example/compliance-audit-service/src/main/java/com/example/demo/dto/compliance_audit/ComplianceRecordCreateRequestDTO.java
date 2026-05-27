package com.example.demo.dto.compliance_audit;

import com.example.demo.model.Enums.ComplianceResult;
import com.example.demo.model.Enums.ComplianceSubjectType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplianceRecordCreateRequestDTO {

	/** PROJECT / PROGRAM / INCENTIVE */
	@NotNull(message = "subjectType is required")
	private ComplianceSubjectType subjectType;

	/** ID of Project / Program / Incentive (from other microservice) */

	@NotNull(message = "subjectId is required")
	@Positive(message = "subjectId must be a positive number")
	private Long subjectId;

	/** PASS / FAIL / NEEDS_REVIEW */
	@NotNull(message = "result is required")
	private ComplianceResult result;

	/** Optional notes entered by compliance officer */

	@Size(max = 5000, message = "Notes cannot exceed 5000 characters")
	private String notes;

	/** Optional evidence link */
	private String evidenceURL;
}
