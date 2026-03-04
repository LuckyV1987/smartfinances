package com.smartfinances.dto.response;

import com.smartfinances.entity.enums.AccountTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialAccountResponseDTO {
    private Long id;
    private Long ownershipEntityId;
    private String name;
    private String description;
    private AccountTypeEnum type;
    private String currencyCode;
    private boolean active;
    private BigDecimal currentBalance;  // Calculated on the fly from transactions
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

