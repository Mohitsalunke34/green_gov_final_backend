package com.example.demo.modelMapper;

import com.example.demo.dto.IncentiveResponseDTO;
import com.example.demo.model.Incentive;

public class IncentiveMapper {

    private IncentiveMapper() {
       
    }

    public static IncentiveResponseDTO toDTO(Incentive entity) {
        if (entity == null) {
            return null;
        }

        IncentiveResponseDTO dto = new IncentiveResponseDTO();

        
        dto.setIncentiveId(entity.getIncentiveId());
        dto.setApplicationId(entity.getApplicationId());
        dto.setProgramId(entity.getProgramId());
        dto.setBeneficiaryId(entity.getBeneficiaryId());

     
        dto.setAmount(entity.getAmount());
        dto.setRemainingAmount(entity.getRemainingAmount());

        
        dto.setSanctionedDate(entity.getSanctionedDate());
        dto.setStatus(entity.getStatus());

       
        if (entity.getApprovedBy() != null) {
            dto.setApprovedByUserId(entity.getApprovedBy());
        }

        return dto;
    }
}