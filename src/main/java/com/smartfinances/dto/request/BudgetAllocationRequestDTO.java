package com.smartfinances.dto.request;

import com.smartfinances.entity.enums.AllocationIntervalEnum;
import com.smartfinances.entity.enums.AllocationTypeEnum;
import com.smartfinances.entity.enums.AllocationUnitEnum;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAllocationRequestDTO {

    @NotNull(message = "Ownership entity ID is required")
    private Long ownershipEntityId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Type is required")
    private AllocationTypeEnum type;

    @DecimalMin(value = "0.0001", message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    // Required if targetAmount is provided
    private AllocationUnitEnum targetUnit;

    // Null = one-time allocation
    private AllocationIntervalEnum interval;

    // Required if type = SINKING_FUND
    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    private boolean rollover = false;
}

