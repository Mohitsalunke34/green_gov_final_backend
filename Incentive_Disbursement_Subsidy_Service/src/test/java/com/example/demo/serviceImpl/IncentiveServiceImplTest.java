package com.example.demo.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.client.OfficerClient;
import com.example.demo.client.ProgramClient;
import com.example.demo.dto.IncentiveCreateRequestDTO;
import com.example.demo.dto.IncentiveResponseDTO;
import com.example.demo.dto.OfficerDTO;
import com.example.demo.dto.ProgramDTO;
import com.example.demo.dto.client_dto.ApprovedApplicationLookupDTO;
import com.example.demo.exception.InvalidIncentiveException;
import com.example.demo.model.Incentive;
import com.example.demo.repo.DisbursementRepository;
import com.example.demo.repo.IncentiveRepository;
import com.example.demo.service.impl.IncentiveServiceImpl;

@ExtendWith(MockitoExtension.class)
class IncentiveServiceImplTest {

	@InjectMocks
	private IncentiveServiceImpl incentiveService;

	@Mock
	private IncentiveRepository incentiveRepo;

	@Mock
	private ProgramClient programClient;

	@Mock
	private OfficerClient officerClient;

	@Mock
	private DisbursementRepository disbursementRepo;

	private IncentiveCreateRequestDTO dto;

	@BeforeEach
	void setUp() {
		dto = new IncentiveCreateRequestDTO();
		dto.setParticipantId(1L);
		dto.setAmount(1000.0);
	}

	// SUCCESS TEST CASE
	@Test
	void createIncentive_success() {

		Long officerId = 101L;

		//  Mock officer
		OfficerDTO officer = new OfficerDTO();
		officer.setUserId(officerId);
		officer.setUsername("test");

		when(officerClient.getActiveDisbursementOfficers(officerId)).thenReturn(List.of(officer));

		//  Mock application
		ApprovedApplicationLookupDTO application = new ApprovedApplicationLookupDTO();
		application.setApplicationId(10L);
		application.setProgramId(20L);

		when(programClient.getApprovedApplicationsByParticipant(1L)).thenReturn(List.of(application));

		//  No duplicate incentive
		when(incentiveRepo.findByApplicationId(10L)).thenReturn(Optional.empty());

		//  Mock program
		ProgramDTO program = new ProgramDTO();
		program.setStatus("ACTIVE");
		program.setBudget(BigDecimal.valueOf(5000));
		program.setRemainingProgramBudget(BigDecimal.valueOf(5000)); 

		when(programClient.getProgramById(20L)).thenReturn(program);

		doNothing().when(programClient).deductProgramBudget(anyLong(), any(BigDecimal.class));

		// Mock save
		Incentive saved = Incentive.builder().incentiveId(1L).applicationId(10L).programId(20L).beneficiaryId(1L)
				.amount(1000.0).remainingAmount(1000.0).status("APPROVED").build();

		when(incentiveRepo.save(any(Incentive.class))).thenReturn(saved);

	
		IncentiveResponseDTO response = incentiveService.createIncentive(dto, officerId);

		//Assertions
		assertNotNull(response);
		assertEquals(1L, response.getIncentiveId());

		
		verify(programClient)
	    .deductProgramBudget(eq(20L), any(BigDecimal.class));

	}

	
	@Test
	void createIncentive_invalidOfficer() {

		when(officerClient.getActiveDisbursementOfficers(anyLong())).thenReturn(List.of());

		assertThrows(InvalidIncentiveException.class, () -> incentiveService.createIncentive(dto, 101L));
	}

	
	@Test
	void createIncentive_noApplication() {

		Long officerId = 101L;

		OfficerDTO officer = new OfficerDTO();
		officer.setUserId(officerId);

		when(officerClient.getActiveDisbursementOfficers(officerId)).thenReturn(List.of(officer));

		when(programClient.getApprovedApplicationsByParticipant(1L)).thenReturn(List.of());

		assertThrows(InvalidIncentiveException.class, () -> incentiveService.createIncentive(dto, officerId));
	}

	// FAILURE TEST: DUPLICATE
	@Test
	void createIncentive_duplicate() {

		Long officerId = 101L;

		OfficerDTO officer = new OfficerDTO();
		officer.setUserId(officerId);

		when(officerClient.getActiveDisbursementOfficers(officerId)).thenReturn(List.of(officer));

		ApprovedApplicationLookupDTO application = new ApprovedApplicationLookupDTO();
		application.setApplicationId(10L);
		application.setProgramId(20L);

		when(programClient.getApprovedApplicationsByParticipant(1L)).thenReturn(List.of(application));

		when(incentiveRepo.findByApplicationId(10L)).thenReturn(Optional.of(new Incentive()));

		assertThrows(InvalidIncentiveException.class, () -> incentiveService.createIncentive(dto, officerId));
	}
}