package com.smartfinances.mapper;

import com.smartfinances.dto.request.SpendCategoryRequestDTO;
import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.SpendCategory;
import org.springframework.stereotype.Component;

@Component
public class SpendCategoryMapper {

    public SpendCategoryResponseDTO toResponseDTO(SpendCategory entity) {
        return SpendCategoryResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .description(entity.getDescription())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SpendCategory toEntity(SpendCategoryRequestDTO dto) {
        return SpendCategory.builder()
                .name(dto.getName())
                .type(dto.getType())
                .description(dto.getDescription())
                .build();
    }

    public void updateEntityFromDTO(SpendCategoryRequestDTO dto, SpendCategory entity) {
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getType() != null) {
            entity.setType(dto.getType());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
    }
}

