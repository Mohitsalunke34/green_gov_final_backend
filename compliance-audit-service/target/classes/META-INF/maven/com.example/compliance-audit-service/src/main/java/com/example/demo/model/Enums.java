package com.example.demo.model;

public class Enums {
	public enum ComplianceResult {
		PASS, FAIL, NEEDS_REVIEW
	}

	public enum ComplianceSubjectType {
		PROJECT, PROGRAM, INCENTIVE
	}

	public enum AuditStatus {
		IN_PROGRESS, COMPLETED
	}

	public enum ComplianceAuditStatus {
		PENDING, VERIFIED, FLAGGED
	}

	public enum ReportScope {
		PROJECT, PROGRAM, INCENTIVE
	}
}
