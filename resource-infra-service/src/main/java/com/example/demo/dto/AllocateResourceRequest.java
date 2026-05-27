package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AllocateResourceRequest {

    @NotNull
    @Positive
    private Double allocationQuantity;

    private String remarks;
    
    private Long userId;
}