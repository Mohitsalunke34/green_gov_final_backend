
package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.client.OfficerClient;
import com.example.demo.client.ProgramClient;
import com.example.demo.dto.IncentiveCreateRequestDTO;
import com.example.demo.dto.IncentiveResponseDTO;
import com.example.demo.dto.OfficerDTO;
import com.example.demo.dto.ProgramDTO;
import com.example.demo.dto.client_dto.ApprovedApplicationLookupDTO;
import com.example.demo.exception.InvalidIncentiveException;
import com.example.demo.model.Incentive;
import com.example.demo.modelMapper.IncentiveMapper;
import com.example.demo.repo.DisbursementRepository;
import com.example.demo.repo.IncentiveRepository;
import com.example.demo.service.IncentiveService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class IncentiveServiceImpl implements IncentiveService {

	private final IncentiveRepository incentiveRepo;
	private final ProgramClient programClient;
	private final OfficerClient officerClient;
	private final DisbursementRepository disbursementRepo;

	@Override
	@Transactional
	@CircuitBreaker(name = "programService", fallbackMethod = "createIncentiveFallback")
	public IncentiveResponseDTO createIncentive(
	        IncentiveCreateRequestDTO dto,
	        Long officerUserId) {

	    List<OfficerDTO> officers =
	            officerClient.getActiveDisbursementOfficers(officerUserId);

	    OfficerDTO officer = officers.stream()
	            .filter(o -> o.getUserId().equals(officerUserId))
	            .findFirst()
	            .orElseThrow(() ->
	                    new InvalidIncentiveException("User is not an approved disbursement officer"));

	    log.info("Creating incentive by officer {}", officer.getUsername());

	    List<ApprovedApplicationLookupDTO> applications =
	            programClient.getApprovedApplicationsByParticipant(dto.getParticipantId());

	    if (applications.isEmpty()) {
	        throw new InvalidIncentiveException("No approved application found for this participant");
	    }

	    ApprovedApplicationLookupDTO application = applications.get(0);

	    Long applicationId = application.getApplicationId();
	    Long programId = application.getProgramId();

	    incentiveRepo.findByApplicationId(applicationId)
	            .ifPresent(existing -> {
	                throw new InvalidIncentiveException("Incentive already exists for this application");
	            });

	    ProgramDTO program = programClient.getProgramById(programId);

	    if (!"ACTIVE".equalsIgnoreCase(program.getStatus())) {
	        throw new InvalidIncentiveException("Program is not active");
	    }

	    BigDecimal requestedAmount = BigDecimal.valueOf(dto.getAmount());

	    BigDecimal remainingBudget =
	            program.getRemainingProgramBudget() != null
	                    ? program.getRemainingProgramBudget()
	                    : program.getBudget();

	    if (requestedAmount.compareTo(remainingBudget) > 0) {
	        throw new InvalidIncentiveException("Insufficient program budget");
	    }

	    
	    programClient.deductProgramBudget(programId, requestedAmount);

	    Incentive incentive = Incentive.builder()
	            .applicationId(applicationId)
	            .programId(programId)
	            .beneficiaryId(dto.getParticipantId())
	            .amount(dto.getAmount())
	            .remainingAmount(dto.getAmount())
	            .sanctionedDate(LocalDate.now())
	            .status("APPROVED")
	            .approvedBy(officerUserId)
	            .build();

	    Incentive saved = incentiveRepo.save(incentive);

	    log.info("Incentive created | IncentiveId={} | ParticipantId={}",
	            saved.getIncentiveId(), dto.getParticipantId());

	    return IncentiveMapper.toDTO(saved);
	}
	
	public IncentiveResponseDTO createIncentiveFallback(IncentiveCreateRequestDTO dto,
	        Long officerUserId,Exception ex) {

	    log.error("Fallback triggered for createIncentive", ex);

	    throw new InvalidIncentiveException(
	            "Service temporarily unavailable. Please try again later."
	    );
	}
	       


	@Override
	public List<IncentiveResponseDTO> getByBeneficiary(Long beneficiaryId) {
		return incentiveRepo.findByBeneficiaryId(beneficiaryId).stream().map(IncentiveMapper::toDTO).toList();
	}

	@Override
	public IncentiveResponseDTO getByIncentiveId(Long incentiveId) {
		return incentiveRepo.findByIncentiveId(incentiveId).map(IncentiveMapper::toDTO)
				.orElseThrow(() -> new InvalidIncentiveException("Incentive not found"));
	}

	@Override
	@Transactional
	public IncentiveResponseDTO deleteIncentive(Long incentiveId) {
		Incentive incentive = incentiveRepo.findById(incentiveId)
				.orElseThrow(() -> new IllegalArgumentException("Incentive not found"));
		IncentiveResponseDTO response = IncentiveMapper.toDTO(incentive);
		incentiveRepo.delete(incentive);
		return response;
	}

	@Override
	public List<IncentiveResponseDTO> getAllIncentives() {
		return incentiveRepo.findAll().stream().map(IncentiveMapper::toDTO).toList();
	}

	@Override
	public Map<String, Object> getIncentiveReportMetrics() {

		long totalIncentives = incentiveRepo.count();
		long totalDisbursements = disbursementRepo.count();
		Double totalDisbursedAmount = disbursementRepo.getTotalDisbursedAmount();

		Map<String, Object> metrics = new HashMap<>();
		metrics.put("totalIncentives", totalIncentives);
		metrics.put("totalDisbursements", totalDisbursements);
		metrics.put("totalAmountDisbursed", totalDisbursedAmount != null ? totalDisbursedAmount : 0.0);

		return metrics;
	}

	@Override
	@Transactional
	public boolean incentiveExists(Long incentiveId) {
		return incentiveRepo.existsById(incentiveId);
	}

}