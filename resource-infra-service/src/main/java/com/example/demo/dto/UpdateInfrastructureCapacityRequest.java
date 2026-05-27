package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class UpdateInfrastructureCapacityRequest {
	@NotNull
    @PositiveOrZero
    private Double utilizedCapacity;

	private String remarks;

	private Long userId;
}
