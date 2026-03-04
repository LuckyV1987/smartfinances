package com.smartfinances.dto.response;

import com.smartfinances.entity.enums.OwnershipEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipEntityResponseDTO {
    private Long id;
    private String name;
    private OwnershipEntityType type;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

