package com.example.demo.complianceTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.demo.clients.NotificationClient;
import com.example.demo.dto.UserBasicDTO;
import com.example.demo.dto.compliance_audit.ComplianceRecordCreateRequestDTO;
import com.example.demo.model.ComplianceRecord;
import com.example.demo.model.Enums.ComplianceAuditStatus;
import com.example.demo.model.Enums.ComplianceResult;
import com.example.demo.model.Enums.ComplianceSubjectType;
import com.example.demo.repo.ComplianceRecordRepository;
import com.example.demo.service.ComplianceServiceImpl;

class CreateComplianceTest {

	@Mock // Create a FAKE version of this object
	private ComplianceRecordRepository complianceRepo;

	@Mock
	private NotificationClient notificationClient;

	@InjectMocks // Real object of your service, Automatically injects mocks into it:
	private ComplianceServiceImpl complianceService;

	@BeforeEach // Run this before every test
	void setup() {
		MockitoAnnotations.openMocks(this); // It initializes mock and injectmock, without this all mocks null.
	}

	// TEST 1: SUCCESSFUL CREATION
	@Test // Make this as a test case, JUnit will run it automatically
	void testCreateCompliance_success() {

		// This simulates frontend request
		ComplianceRecordCreateRequestDTO dto = new ComplianceRecordCreateRequestDTO();
		dto.setSubjectType(ComplianceSubjectType.PROJECT);
		dto.setSubjectId(1L);
		dto.setResult(ComplianceResult.PASS);
		dto.setNotes("Test notes");

		Long userId = 3L;

		UserBasicDTO mockUser = new UserBasicDTO();
		mockUser.setUsername("testUser"); // This simulates auth service response

		// Use spy for internal methods
		ComplianceServiceImpl spyService = spy(complianceService); // Real object + partial mocking

		doReturn(mockUser).when(spyService).fetchOfficer(userId); // When fetchOfficer is called → return mockUser (It
																	// prevents original fiegn call)
		doNothing().when(spyService).validateSubject(any(), any()); // Skip validation logic, Don’t call external
																	// services

		// Don’t actually send notification
		doNothing().when(notificationClient).createNotification(any());

		// No duplicate compliance returns no compliance exists
		when(complianceRepo.existsBySubjectTypeAndSubjectIdAndAuditStatusIn(any(), anyLong(), anyList()))
				.thenReturn(false);

		// Mock DB save
		ComplianceRecord saved = new ComplianceRecord();
		saved.setId(1L);

		saved.setSubjectType(ComplianceSubjectType.PROJECT);
		saved.setSubjectId(1L);
		saved.setAuditStatus(ComplianceAuditStatus.PENDING);
		saved.setResult(ComplianceResult.PASS);

		when(complianceRepo.save(any(ComplianceRecord.class))).thenReturn(saved);

		// real business method for creating recordCompliance through mock service.
		var result = spyService.recordCompliance(dto, userId);

		// Output must NOT be null
		assertNotNull(result);

		// Check save() was called exactly ONCE
		verify(complianceRepo, times(1)).save(any());
//		Check notification was triggered
		verify(notificationClient, times(1)).createNotification(any());
	}

	// TEST 2: DUPLICATE COMPLIANCE
	@Test
	void testCreateCompliance_duplicate_shouldThrowException() {

		// Arrange
		ComplianceRecordCreateRequestDTO dto = new ComplianceRecordCreateRequestDTO();
		dto.setSubjectType(ComplianceSubjectType.PROJECT);
		dto.setSubjectId(1L);
		dto.setResult(ComplianceResult.PASS);

		Long userId = 3L;

		ComplianceServiceImpl spyService = spy(complianceService);

		doReturn(new UserBasicDTO()).when(spyService).fetchOfficer(userId);
		doNothing().when(spyService).validateSubject(any(), any());

		doNothing().when(notificationClient).createNotification(any());

		// Simulate duplicate
		when(complianceRepo.existsBySubjectTypeAndSubjectIdAndAuditStatusIn(any(), anyLong(), anyList()))
				.thenReturn(true);

		// Act + Assert
		assertThrows(IllegalStateException.class, () -> {
			spyService.recordCompliance(dto, userId);
		});

		// Ensure save not called
		verify(complianceRepo, never()).save(any());
	}
}