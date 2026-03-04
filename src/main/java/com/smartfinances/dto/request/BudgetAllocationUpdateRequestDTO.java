package com.smartfinances.dto.request;

import com.smartfinances.entity.enums.AllocationIntervalEnum;
import com.smartfinances.entity.enums.AllocationUnitEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
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
public class BudgetAllocationUpdateRequestDTO {

    // All fields optional for updates
    // ownershipEntityId and type are immutable and ignored

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @DecimalMin(value = "0.0001", message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    private AllocationUnitEnum targetUnit;

    private AllocationIntervalEnum interval;

    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    private Boolean rollover;
}

