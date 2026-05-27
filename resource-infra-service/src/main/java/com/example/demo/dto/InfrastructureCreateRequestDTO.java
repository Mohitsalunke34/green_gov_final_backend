package com.example.demo.dto;

import enums.InfrastructureType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InfrastructureCreateRequestDTO {

	@NotNull
    private Long projectId;

    @NotBlank
    private String infrastructureName;

    @NotNull
    private InfrastructureType type;

    @NotBlank
    private String location;

    @NotNull
    @Positive
    private Double capacity;
}