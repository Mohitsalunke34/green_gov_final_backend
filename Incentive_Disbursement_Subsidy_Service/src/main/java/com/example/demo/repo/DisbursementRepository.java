package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Disbursement;
import com.example.demo.model.Incentive;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {


    List<Disbursement> findByIncentive(Incentive incentive);

//    List<Disbursement> findByOfficerUserId(Long officerUserId);
    
    List<Disbursement> findByIncentive_IncentiveIdIn(List<Long> incentiveIds);
    
	@Query("SELECT COALESCE(SUM(d.amount), 0) FROM Disbursement d")
	Double getTotalDisbursedAmount();

}