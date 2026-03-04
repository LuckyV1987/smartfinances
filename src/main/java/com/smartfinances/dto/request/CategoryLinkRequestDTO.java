package com.smartfinances.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryLinkRequestDTO {

    @NotNull(message = "Spend category ID is required")
    private Long spendCategoryId;
}

