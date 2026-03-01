package com.smartfinances.dto.response;

import com.smartfinances.entity.enums.SpendCategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendCategoryResponseDTO {
    private Long id;
    private String name;
    private SpendCategoryType type;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

