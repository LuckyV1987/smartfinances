package com.smartfinances.dto.request;

import com.smartfinances.entity.enums.SpendCategoryType;
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
public class SpendCategoryRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name too long")
    private String name;

    @NotNull(message = "Type is required")
    private SpendCategoryType type;

    @Size(max = 255, message = "Description too long")
    private String description;
}

