package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IncentiveCreateRequestDTO {

	@NotNull
	private Long participantId;
	

	@NotNull(message = "Amount is required")
	@Min(value = 0, message = "Amount must be positive")
	private Double amount;
}