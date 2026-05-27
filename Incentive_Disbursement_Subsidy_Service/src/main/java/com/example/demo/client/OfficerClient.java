package com.example.demo.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.demo.dto.OfficerDTO;


@FeignClient(name = "AUTH-SERVICE")
public interface OfficerClient {

	@GetMapping("/api/admin/officers/disbursement/active")
	List<OfficerDTO> getActiveDisbursementOfficers(@RequestHeader("X-Officer-User-Id") Long officerUserId);
}
