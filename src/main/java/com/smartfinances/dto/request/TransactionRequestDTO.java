package com.smartfinances.dto.request;

import com.smartfinances.entity.enums.TransactionTypeEnum;
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
public class TransactionRequestDTO {

    @NotNull(message = "Ownership entity ID is required")
    private Long ownershipEntityId;

    @NotNull(message = "Financial account ID is required")
    private Long financialAccountId;

    @NotNull(message = "Spend category ID is required")
    private Long spendCategoryId;

    // Optional
    private Long budgetAllocationId;

    @NotNull(message = "Transaction type is required")
    private TransactionTypeEnum type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;
}
