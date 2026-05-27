package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.model.EnergyProgram;
import com.example.demo.model.ProgramApplication;

import feign.Param;

public interface ProgramApplicationRepository extends JpaRepository<ProgramApplication, Long> {

	List<ProgramApplication> findByApplicantId(Long applicantId);

	List<ProgramApplication> findByProgram(EnergyProgram program);

	Optional<ProgramApplication> findByApplicantIdAndProgram(Long applicantId, EnergyProgram program);

	List<ProgramApplication> findByApplicantIdAndStatus(Long applicantId, String status);

	@Modifying
    @Query("UPDATE ProgramApplication p SET p.incentiveId = :incentiveId WHERE p.applicationId = :applicationId")
    int updateIncentiveId(@Param("applicationId") Long applicationId, @Param("incentiveId") Long incentiveId);
}