package com.smartfinances.service;

import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.BudgetAllocation;
import com.smartfinances.entity.BudgetAllocationCategory;
import com.smartfinances.entity.SpendCategory;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.SpendCategoryMapper;
import com.smartfinances.repository.BudgetAllocationCategoryRepository;
import com.smartfinances.repository.BudgetAllocationRepository;
import com.smartfinances.repository.SpendCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetAllocationCategoryService {

    private final BudgetAllocationCategoryRepository budgetAllocationCategoryRepository;
    private final BudgetAllocationRepository budgetAllocationRepository;
    private final SpendCategoryRepository spendCategoryRepository;
    private final SpendCategoryMapper spendCategoryMapper;

    public BudgetAllocationCategoryService(
            BudgetAllocationCategoryRepository budgetAllocationCategoryRepository,
            BudgetAllocationRepository budgetAllocationRepository,
            SpendCategoryRepository spendCategoryRepository,
            SpendCategoryMapper spendCategoryMapper) {
        this.budgetAllocationCategoryRepository = budgetAllocationCategoryRepository;
        this.budgetAllocationRepository = budgetAllocationRepository;
        this.spendCategoryRepository = spendCategoryRepository;
        this.spendCategoryMapper = spendCategoryMapper;
    }

    @Transactional(readOnly = true)
    public List<SpendCategoryResponseDTO> findCategoriesByAllocation(Long allocationId) {
        // Verify allocation exists and is active
        budgetAllocationRepository.findByIdAndActiveTrue(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + allocationId));

        return budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId)
                .stream()
                .map(bac -> spendCategoryMapper.toResponseDTO(bac.getSpendCategory()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<SpendCategoryResponseDTO> linkCategory(Long allocationId, Long spendCategoryId) {
        // Verify allocation exists and is active
        BudgetAllocation allocation = budgetAllocationRepository.findByIdAndActiveTrue(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + allocationId));

        // Verify spend category exists and is active
        SpendCategory spendCategory = spendCategoryRepository.findById(spendCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Spend category not found with id: " + spendCategoryId));

        if (!spendCategory.isActive()) {
            throw new ResourceNotFoundException("Spend category is inactive");
        }

        // Check if already linked
        if (budgetAllocationCategoryRepository.existsByBudgetAllocationIdAndSpendCategoryId(allocationId, spendCategoryId)) {
            throw new DuplicateResourceException("Category is already linked to this allocation");
        }

        // Create link
        BudgetAllocationCategory link = BudgetAllocationCategory.builder()
                .budgetAllocation(allocation)
                .spendCategory(spendCategory)
                .build();

        budgetAllocationCategoryRepository.save(link);

        // Return updated category list
        return findCategoriesByAllocation(allocationId);
    }

    @Transactional
    public List<SpendCategoryResponseDTO> unlinkCategory(Long allocationId, Long spendCategoryId) {
        // Verify allocation exists and is active
        budgetAllocationRepository.findByIdAndActiveTrue(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + allocationId));

        // Check if link exists
        if (!budgetAllocationCategoryRepository.existsByBudgetAllocationIdAndSpendCategoryId(allocationId, spendCategoryId)) {
            throw new ResourceNotFoundException("Category link not found");
        }

        // Delete link
        budgetAllocationCategoryRepository.deleteByBudgetAllocationIdAndSpendCategoryId(allocationId, spendCategoryId);

        // Return updated category list
        return findCategoriesByAllocation(allocationId);
    }
}

