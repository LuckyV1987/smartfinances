package com.smartfinances.mapper;

import com.smartfinances.dto.request.BudgetAllocationRequestDTO;
import com.smartfinances.dto.request.BudgetAllocationUpdateRequestDTO;
import com.smartfinances.dto.response.BudgetAllocationResponseDTO;
import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.BudgetAllocation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BudgetAllocationMapper {

    /**
     * Maps entity + linked categories + calculated balance to response DTO.
     * remainingAmount = targetAmount - currentBalance if targetAmount is set, else null.
     */
    public BudgetAllocationResponseDTO toResponseDTO(
            BudgetAllocation entity,
            List<SpendCategoryResponseDTO> categories,
            BigDecimal currentBalance) {

        BigDecimal remainingAmount = null;
        if (entity.getTargetAmount() != null && currentBalance != null) {
            remainingAmount = entity.getTargetAmount().subtract(currentBalance);
        }

        return BudgetAllocationResponseDTO.builder()
                .id(entity.getId())
                .ownershipEntityId(entity.getOwnershipEntity().getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .targetAmount(entity.getTargetAmount())
                .targetUnit(entity.getTargetUnit())
                .interval(entity.getInterval())
                .targetDate(entity.getTargetDate())
                .rollover(entity.isRollover())
                .active(entity.isActive())
                .currentBalance(currentBalance)
                .remainingAmount(remainingAmount)
                .categories(categories)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Maps request DTO to new entity.
     * Does not set ownershipEntity (resolved in service via repository lookup).
     * Does not set categories (managed via join table).
     */
    public BudgetAllocation toEntity(BudgetAllocationRequestDTO dto) {
        return BudgetAllocation.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .targetAmount(dto.getTargetAmount())
                .targetUnit(dto.getTargetUnit())
                .interval(dto.getInterval())
                .targetDate(dto.getTargetDate())
                .rollover(dto.isRollover())
                .active(true)
                .build();
    }

    /**
     * Updates only non-null fields on existing entity.
     * Never updates: createdAt, ownershipEntityId, type.
     */
    public void updateEntityFromDTO(BudgetAllocationUpdateRequestDTO dto, BudgetAllocation entity) {
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        // type is immutable - never update
        if (dto.getTargetAmount() != null) {
            entity.setTargetAmount(dto.getTargetAmount());
        }
        if (dto.getTargetUnit() != null) {
            entity.setTargetUnit(dto.getTargetUnit());
        }
        if (dto.getInterval() != null) {
            entity.setInterval(dto.getInterval());
        }
        if (dto.getTargetDate() != null) {
            entity.setTargetDate(dto.getTargetDate());
        }
        if (dto.getRollover() != null) {
            entity.setRollover(dto.getRollover());
        }
    }
}

