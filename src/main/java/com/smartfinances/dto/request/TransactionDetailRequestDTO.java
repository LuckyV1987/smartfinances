package com.smartfinances.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetailRequestDTO {

    @NotBlank(message = "Item name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0001", message = "Unit price must be greater than zero")
    private BigDecimal unitPrice;

    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.0001", message = "Total price must be greater than zero")
    private BigDecimal totalPrice;

    @Size(max = 255, message = "Notes must not exceed 255 characters")
    private String notes;
}

