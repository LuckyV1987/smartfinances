package com.smartfinances.dto.request;

import com.smartfinances.entity.enums.AccountTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialAccountRequestDTO {

    @NotNull(message = "Ownership entity ID is required")
    private Long ownershipEntityId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Account type is required")
    private AccountTypeEnum type;

    // Defaults to USD if not provided
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    @Builder.Default
    private String currencyCode = "USD";
}

