package com.smartfinances.service;

import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.BudgetAllocation;
import com.smartfinances.entity.BudgetAllocationCategory;
import com.smartfinances.entity.SpendCategory;
import com.smartfinances.entity.enums.SpendCategoryType;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.SpendCategoryMapper;
import com.smartfinances.repository.BudgetAllocationCategoryRepository;
import com.smartfinances.repository.BudgetAllocationRepository;
import com.smartfinances.repository.SpendCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetAllocationCategoryServiceTest {

    @Mock
    private BudgetAllocationCategoryRepository budgetAllocationCategoryRepository;

    @Mock
    private BudgetAllocationRepository budgetAllocationRepository;

    @Mock
    private SpendCategoryRepository spendCategoryRepository;

    @Mock
    private SpendCategoryMapper spendCategoryMapper;

    @InjectMocks
    private BudgetAllocationCategoryService budgetAllocationCategoryService;

    @Test
    void shouldReturnCategories_whenFindByAllocation() {
        // arrange
        Long allocationId = 1L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .active(true)
                .build();

        SpendCategory category1 = SpendCategory.builder()
                .id(1L)
                .name("Groceries")
                .type(SpendCategoryType.EXPENSE)
                .build();

        SpendCategory category2 = SpendCategory.builder()
                .id(2L)
                .name("Dining")
                .type(SpendCategoryType.EXPENSE)
                .build();

        BudgetAllocationCategory link1 = BudgetAllocationCategory.builder()
                .id(1L)
                .budgetAllocation(allocation)
                .spendCategory(category1)
                .build();

        BudgetAllocationCategory link2 = BudgetAllocationCategory.builder()
                .id(2L)
                .budgetAllocation(allocation)
                .spendCategory(category2)
                .build();

        SpendCategoryResponseDTO dto1 = SpendCategoryResponseDTO.builder()
                .id(1L)
                .name("Groceries")
                .build();

        SpendCategoryResponseDTO dto2 = SpendCategoryResponseDTO.builder()
                .id(2L)
                .name("Dining")
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId))
                .thenReturn(Arrays.asList(link1, link2));
        when(spendCategoryMapper.toResponseDTO(category1)).thenReturn(dto1);
        when(spendCategoryMapper.toResponseDTO(category2)).thenReturn(dto2);

        // act
        List<SpendCategoryResponseDTO> result = budgetAllocationCategoryService.findCategoriesByAllocation(allocationId);

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Groceries");
        assertThat(result.get(1).getName()).isEqualTo("Dining");
        verify(budgetAllocationRepository).findByIdAndActiveTrue(allocationId);
        verify(budgetAllocationCategoryRepository).findByBudgetAllocationId(allocationId);
    }

    @Test
    void shouldLinkCategory_whenValidRequest() {
        // arrange
        Long allocationId = 1L;
        Long categoryId = 2L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .active(true)
                .build();

        SpendCategory category = SpendCategory.builder()
                .id(categoryId)
                .name("Groceries")
                .active(true)
                .build();

        BudgetAllocationCategory link = BudgetAllocationCategory.builder()
                .budgetAllocation(allocation)
                .spendCategory(category)
                .build();

        SpendCategoryResponseDTO dto = SpendCategoryResponseDTO.builder()
                .id(categoryId)
                .name("Groceries")
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(spendCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(budgetAllocationCategoryRepository.existsByBudgetAllocationIdAndSpendCategoryId(allocationId, categoryId))
                .thenReturn(false);
        when(budgetAllocationCategoryRepository.save(any(BudgetAllocationCategory.class))).thenReturn(link);
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId)).thenReturn(List.of(link));
        when(spendCategoryMapper.toResponseDTO(category)).thenReturn(dto);

        // act
        List<SpendCategoryResponseDTO> result = budgetAllocationCategoryService.linkCategory(allocationId, categoryId);

        // assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Groceries");
        verify(budgetAllocationCategoryRepository).save(any(BudgetAllocationCategory.class));
    }

    @Test
    void shouldThrowException_whenCategoryAlreadyLinked() {
        // arrange
        Long allocationId = 1L;
        Long categoryId = 2L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .active(true)
                .build();

        SpendCategory category = SpendCategory.builder()
                .id(categoryId)
                .active(true)
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(spendCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(budgetAllocationCategoryRepository.existsByBudgetAllocationIdAndSpendCategoryId(allocationId, categoryId))
                .thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> budgetAllocationCategoryService.linkCategory(allocationId, categoryId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already linked");

        verify(budgetAllocationCategoryRepository, never()).save(any());
    }

    @Test
    void shouldUnlinkCategory_whenValidRequest() {
        // arrange
        Long allocationId = 1L;
        Long categoryId = 2L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .active(true)
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationCategoryRepository.existsByBudgetAllocationIdAndSpendCategoryId(allocationId, categoryId))
                .thenReturn(true);
        doNothing().when(budgetAllocationCategoryRepository).deleteByBudgetAllocationIdAndSpendCategoryId(allocationId, categoryId);
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId)).thenReturn(List.of());

        // act
        List<SpendCategoryResponseDTO> result = budgetAllocationCategoryService.unlinkCategory(allocationId, categoryId);

        // assert
        assertThat(result).isEmpty();
        verify(budgetAllocationCategoryRepository).deleteByBudgetAllocationIdAndSpendCategoryId(allocationId, categoryId);
    }

    @Test
    void shouldThrowException_whenCategoryLinkNotFound() {
        // arrange
        Long allocationId = 1L;
        Long categoryId = 99L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .active(true)
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationCategoryRepository.existsByBudgetAllocationIdAndSpendCategoryId(allocationId, categoryId))
                .thenReturn(false);

        // act & assert
        assertThatThrownBy(() -> budgetAllocationCategoryService.unlinkCategory(allocationId, categoryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category link not found");

        verify(budgetAllocationCategoryRepository, never()).deleteByBudgetAllocationIdAndSpendCategoryId(anyLong(), anyLong());
    }
}

