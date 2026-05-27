package com.example.demo.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.client.ParticipantClient;
import com.example.demo.dto.DisbursementProcessResponse;
import com.example.demo.dto.DisbursementResponseDTO;
import com.example.demo.dto.ParticipantBasicDTO;
import com.example.demo.dto.ProgramDTO;
import com.example.demo.exception.InvalidDisbursementException;
import com.example.demo.exception.InvalidIncentiveException;
import com.example.demo.model.Disbursement;
import com.example.demo.model.Incentive;
import com.example.demo.modelMapper.DisbursementMapper;
import com.example.demo.repo.DisbursementRepository;
import com.example.demo.repo.IncentiveRepository;
import com.example.demo.service.DisbursementService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class DisbursementServiceImpl implements DisbursementService {

	private final DisbursementRepository disbursementRepo;
	private final IncentiveRepository incentiveRepo;
	
	private final ParticipantClient participantClient;

	@Override
	public DisbursementProcessResponse disburse(Long incentiveId, Double amount, Long officerUserId) {

		log.info("Disbursement | IncentiveId={} | Amount={}", incentiveId, amount);

		Incentive incentive = incentiveRepo.findById(incentiveId)
				.orElseThrow(() ->
                new InvalidIncentiveException("Incentive not found"));


		if (!"APPROVED".equals(incentive.getStatus()) && !"PARTIALLY_DISBURSED".equals(incentive.getStatus())) {
			throw new InvalidDisbursementException("Incentive not eligible");
		}

		if (amount > incentive.getRemainingAmount()) {
			throw new InvalidDisbursementException("Amount exceeds remaining balance");
		}

	
		double remaining = incentive.getRemainingAmount() - amount;
		incentive.setRemainingAmount(remaining);

		if (remaining == 0) {
			incentive.setStatus("COMPLETED");
		} else {
			incentive.setStatus("PARTIALLY_DISBURSED");
		}

	
		Disbursement disbursement = new Disbursement();
		disbursement.setIncentive(incentive);
		disbursement.setOfficerUserId(officerUserId);
		disbursement.setAmount(amount);
		disbursement.setPaymentDate(LocalDate.now());
		disbursement.setStatus("SUCCESS");

		incentiveRepo.save(incentive);
		Disbursement saved = disbursementRepo.save(disbursement);

		
		List<DisbursementResponseDTO> history = disbursementRepo.findByIncentive(incentive).stream()
				.map(DisbursementMapper::toDTO).toList();

		return new DisbursementProcessResponse(DisbursementMapper.toDTO(saved), incentive.getAmount(),
				remaining,
				history);

	}

	@Override
	public List<DisbursementResponseDTO> getAllDisbursement(Long incentiveId) {

		Incentive incentive = incentiveRepo.findById(incentiveId)
				.orElseThrow(() -> new InvalidIncentiveException("Incentive not found"));

		List<Disbursement> list = disbursementRepo.findByIncentive(incentive);

		if (list.isEmpty()) {
			throw new InvalidDisbursementException("No disbursements found");
		}

		return list.stream().map(DisbursementMapper::toDTO).toList();
	}

	public List<DisbursementResponseDTO> getByParticipantName(String name) {

		List<ParticipantBasicDTO> participants = participantClient.getParticipants();

		ParticipantBasicDTO participant = participants.stream().filter(p -> p.getLegalName().equalsIgnoreCase(name))
				.findFirst().orElseThrow(() -> new InvalidDisbursementException("Participant not found"));

		Long participantId = participant.getId();

		List<Incentive> incentives = incentiveRepo.findByBeneficiaryId(participantId);

		if (incentives.isEmpty()) {
			throw new InvalidDisbursementException("No incentives found");
		}

		List<Long> ids = incentives.stream().map(Incentive::getIncentiveId).toList();

		List<Disbursement> disbursements = disbursementRepo.findByIncentive_IncentiveIdIn(ids);

		if (disbursements.isEmpty()) {
			throw new InvalidDisbursementException("No disbursements found");
		}

		return disbursements.stream().map(DisbursementMapper::toDTO).toList();
	}

	private ProgramDTO programFallback(Long programId, Throwable ex) {
		log.error("Program service unavailable", ex);
		throw new InvalidDisbursementException("Program service unavailable");
	}
}