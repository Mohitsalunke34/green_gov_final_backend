package com.example.demo.repo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Incentive;

@Repository
public interface IncentiveRepository extends JpaRepository<Incentive, Long> {

	Optional<Incentive> findByApplicationId(Long applicationId);


	List<Incentive> findByBeneficiaryId(Long beneficiaryId);


	List<Incentive> findByProgramId(Long programId);


	Optional<Incentive> findByIncentiveId(Long incentiveId);

	void deleteByIncentiveId(Long incentiveId);

	@Query("SELECT SUM(i.amount) FROM Incentive i WHERE i.programId = :programId")
	BigDecimal sumAmountByProgramId(@Param("programId") Long programId);

	Long countByStatusIn(List<String> statuses);

	@Query("SELECT COALESCE(SUM(i.amount), 0) FROM Incentive i")
	Double sumTotalAmount();

	@Query("SELECT COALESCE(SUM(i.amount - i.remainingAmount), 0) FROM Incentive i")
	Double sumDisbursedAmount();
}