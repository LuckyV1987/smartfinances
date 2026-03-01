package com.smartfinances.service;

import com.smartfinances.dto.request.SpendCategoryRequestDTO;
import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.SpendCategory;
import com.smartfinances.entity.enums.SpendCategoryType;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.SpendCategoryMapper;
import com.smartfinances.repository.SpendCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpendCategoryService {

    private final SpendCategoryRepository spendCategoryRepository;
    private final SpendCategoryMapper spendCategoryMapper;

    public SpendCategoryService(SpendCategoryRepository spendCategoryRepository, SpendCategoryMapper spendCategoryMapper) {
        this.spendCategoryRepository = spendCategoryRepository;
        this.spendCategoryMapper = spendCategoryMapper;
    }

    @Transactional(readOnly = true)
    public List<SpendCategoryResponseDTO> findAll() {
        return spendCategoryRepository.findByActiveTrue().stream()
                .map(spendCategoryMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpendCategoryResponseDTO findById(Long id) {
        SpendCategory category = spendCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spend category not found with id: " + id));

        if (!category.isActive()) {
            throw new ResourceNotFoundException("Spend category not found with id: " + id);
        }

        return spendCategoryMapper.toResponseDTO(category);
    }

    @Transactional(readOnly = true)
    public List<SpendCategoryResponseDTO> findByType(SpendCategoryType type) {
        return spendCategoryRepository.findByTypeAndActiveTrue(type).stream()
                .map(spendCategoryMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpendCategoryResponseDTO create(SpendCategoryRequestDTO dto) {
        // Check name uniqueness
        if (spendCategoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Spend category already exists with name: " + dto.getName());
        }

        // Map and save
        SpendCategory category = spendCategoryMapper.toEntity(dto);
        SpendCategory savedCategory = spendCategoryRepository.save(category);

        return spendCategoryMapper.toResponseDTO(savedCategory);
    }

    @Transactional
    public SpendCategoryResponseDTO update(Long id, SpendCategoryRequestDTO dto) {
        // Find category
        SpendCategory category = spendCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spend category not found with id: " + id));

        // Check name uniqueness if name changed
        if (dto.getName() != null && !dto.getName().equalsIgnoreCase(category.getName())) {
            if (spendCategoryRepository.existsByNameIgnoreCase(dto.getName())) {
                throw new DuplicateResourceException("Spend category already exists with name: " + dto.getName());
            }
        }

        // Update fields
        spendCategoryMapper.updateEntityFromDTO(dto, category);

        // Save and return
        SpendCategory updatedCategory = spendCategoryRepository.save(category);
        return spendCategoryMapper.toResponseDTO(updatedCategory);
    }

    @Transactional
    public void deactivate(Long id) {
        // Find category
        SpendCategory category = spendCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spend category not found with id: " + id));

        // Set inactive
        category.setActive(false);

        // Save
        spendCategoryRepository.save(category);
    }
}

