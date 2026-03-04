package com.smartfinances.mapper;

import com.smartfinances.dto.request.FinancialAccountRequestDTO;
import com.smartfinances.dto.request.FinancialAccountUpdateRequestDTO;
import com.smartfinances.dto.response.FinancialAccountResponseDTO;
import com.smartfinances.entity.FinancialAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FinancialAccountMapper {

    /**
     * Maps entity + calculated balance to response DTO.
     */
    public FinancialAccountResponseDTO toResponseDTO(
            FinancialAccount entity,
            BigDecimal currentBalance) {

        return FinancialAccountResponseDTO.builder()
                .id(entity.getId())
                .ownershipEntityId(entity.getOwnershipEntity().getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .currencyCode(entity.getCurrencyCode())
                .active(entity.isActive())
                .currentBalance(currentBalance)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Maps request DTO to new entity.
     * Does not set ownershipEntity (resolved in service via repository lookup).
     */
    public FinancialAccount toEntity(FinancialAccountRequestDTO dto) {
        return FinancialAccount.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .currencyCode(dto.getCurrencyCode() != null ? dto.getCurrencyCode() : "USD")
                .active(true)
                .build();
    }

    /**
     * Updates only non-null fields on existing entity.
     * Never updates: createdAt, ownershipEntityId, type, currencyCode.
     */
    public void updateEntityFromDTO(FinancialAccountUpdateRequestDTO dto, FinancialAccount entity) {
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        // type, currencyCode, ownershipEntityId are immutable - never update
    }
}

