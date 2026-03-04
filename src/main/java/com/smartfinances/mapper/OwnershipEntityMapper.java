package com.smartfinances.mapper;

import com.smartfinances.dto.request.OwnershipEntityRequestDTO;
import com.smartfinances.dto.request.OwnershipEntityUpdateRequestDTO;
import com.smartfinances.dto.response.MembershipResponseDTO;
import com.smartfinances.dto.response.OwnershipEntityResponseDTO;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.OwnershipMembership;
import org.springframework.stereotype.Component;

@Component
public class OwnershipEntityMapper {

    public OwnershipEntityResponseDTO toResponseDTO(OwnershipEntity entity) {
        return OwnershipEntityResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public OwnershipEntity toEntity(OwnershipEntityRequestDTO dto) {
        return OwnershipEntity.builder()
                .name(dto.getName())
                .type(dto.getType())
                .description(dto.getDescription())
                .active(true)
                .build();
    }

    // Update only non-null fields. Never update createdAt or type.
    public void updateEntityFromDTO(OwnershipEntityUpdateRequestDTO dto, OwnershipEntity entity) {
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        // Type is never updated after creation
    }

    public MembershipResponseDTO toMembershipResponseDTO(OwnershipMembership membership) {
        return MembershipResponseDTO.builder()
                .id(membership.getId())
                .userId(membership.getUser().getId())
                .ownershipEntity(toResponseDTO(membership.getOwnershipEntity()))
                .active(membership.isActive())
                .createdAt(membership.getCreatedAt())
                .updatedAt(membership.getUpdatedAt())
                .build();
    }
}

