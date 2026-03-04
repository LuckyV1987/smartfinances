package com.smartfinances.dto.response;

import com.smartfinances.entity.enums.TransactionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDTO {
    private Long id;
    private Long ownershipEntityId;
    private Long financialAccountId;
    private Long spendCategoryId;
    private String spendCategoryName;
    private Long budgetAllocationId;        // null if not linked
    private TransactionTypeEnum type;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String description;
    private String referenceNumber;
    private boolean systemGenerated;
    private LocalDateTime createdAt;
    // NOTE: details are NOT included here — fetch via GET /{id}/details
}

