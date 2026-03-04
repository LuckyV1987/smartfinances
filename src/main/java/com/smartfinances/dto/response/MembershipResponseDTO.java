package com.smartfinances.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipResponseDTO {
    private Long id;
    private Long userId;
    private OwnershipEntityResponseDTO ownershipEntity;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

