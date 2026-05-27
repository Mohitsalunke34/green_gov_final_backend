package com.example.demo.dto;


import enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
@Data
public class ResourceCreateRequestDTO {

	@NotNull
    private Long projectId;

    @NotBlank
    private String resourceName;

    @NotNull
    private ResourceType type;

    @NotNull
    @Positive
    private Double totalQuantity;

}