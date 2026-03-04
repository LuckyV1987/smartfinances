package com.smartfinances.dto.response;

import com.smartfinances.entity.enums.AllocationIntervalEnum;
import com.smartfinances.entity.enums.AllocationTypeEnum;
import com.smartfinances.entity.enums.AllocationUnitEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAllocationResponseDTO {
    private Long id;
    private Long ownershipEntityId;
    private String name;
    private String description;
    private AllocationTypeEnum type;
    private BigDecimal targetAmount;
    private AllocationUnitEnum targetUnit;
    private AllocationIntervalEnum interval;
    private LocalDate targetDate;
    private boolean rollover;
    private boolean active;
    private BigDecimal currentBalance;    // Calculated on the fly
    private BigDecimal remainingAmount;   // targetAmount - currentBalance; null if no target
    private List<SpendCategoryResponseDTO> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

