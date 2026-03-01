package com.smartfinances.service;

import com.smartfinances.dto.request.SpendCategoryRequestDTO;
import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.SpendCategory;
import com.smartfinances.entity.enums.SpendCategoryType;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.SpendCategoryMapper;
import com.smartfinances.repository.SpendCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpendCategoryServiceTest {

    @Mock
    private SpendCategoryRepository spendCategoryRepository;

    @Mock
    private SpendCategoryMapper spendCategoryMapper;

    @InjectMocks
    private SpendCategoryService spendCategoryService;

    @Test
    void shouldReturnAllActiveCategories_whenFindAll() {
        // arrange
        SpendCategory category1 = SpendCategory.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .active(true)
                .build();

        SpendCategory category2 = SpendCategory.builder()
                .id(2L)
                .name("Groceries")
                .type(SpendCategoryType.EXPENSE)
                .active(true)
                .build();

        SpendCategoryResponseDTO response1 = SpendCategoryResponseDTO.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .active(true)
                .build();

        SpendCategoryResponseDTO response2 = SpendCategoryResponseDTO.builder()
                .id(2L)
                .name("Groceries")
                .type(SpendCategoryType.EXPENSE)
                .active(true)
                .build();

        when(spendCategoryRepository.findByActiveTrue()).thenReturn(List.of(category1, category2));
        when(spendCategoryMapper.toResponseDTO(category1)).thenReturn(response1);
        when(spendCategoryMapper.toResponseDTO(category2)).thenReturn(response2);

        // act
        List<SpendCategoryResponseDTO> result = spendCategoryService.findAll();

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Salary");
        assertThat(result.get(1).getName()).isEqualTo("Groceries");
        verify(spendCategoryRepository).findByActiveTrue();
    }

    @Test
    void shouldReturnCategoriesByType_whenFindByType() {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .active(true)
                .build();

        SpendCategoryResponseDTO response = SpendCategoryResponseDTO.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .build();

        when(spendCategoryRepository.findByTypeAndActiveTrue(SpendCategoryType.INFLOW)).thenReturn(List.of(category));
        when(spendCategoryMapper.toResponseDTO(category)).thenReturn(response);

        // act
        List<SpendCategoryResponseDTO> result = spendCategoryService.findByType(SpendCategoryType.INFLOW);

        // assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(SpendCategoryType.INFLOW);
        verify(spendCategoryRepository).findByTypeAndActiveTrue(SpendCategoryType.INFLOW);
    }

    @Test
    void shouldReturnCategory_whenValidId() {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .active(true)
                .build();

        SpendCategoryResponseDTO response = SpendCategoryResponseDTO.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .build();

        when(spendCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(spendCategoryMapper.toResponseDTO(category)).thenReturn(response);

        // act
        SpendCategoryResponseDTO result = spendCategoryService.findById(1L);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Salary");
        verify(spendCategoryRepository).findById(1L);
    }

    @Test
    void shouldThrowException_whenCategoryNotFound() {
        // arrange
        when(spendCategoryRepository.findById(999L)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> spendCategoryService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Spend category not found");

        verify(spendCategoryRepository).findById(999L);
    }

    @Test
    void shouldThrowException_whenCategoryInactive() {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .id(1L)
                .name("Salary")
                .active(false)
                .build();

        when(spendCategoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // act & assert
        assertThatThrownBy(() -> spendCategoryService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Spend category not found");

        verify(spendCategoryRepository).findById(1L);
    }

    @Test
    void shouldCreateCategory_whenValidRequest() {
        // arrange
        SpendCategoryRequestDTO requestDTO = SpendCategoryRequestDTO.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .description("Regular employment income")
                .build();

        SpendCategory category = SpendCategory.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .description("Regular employment income")
                .active(true)
                .build();

        SpendCategoryResponseDTO response = SpendCategoryResponseDTO.builder()
                .id(1L)
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .description("Regular employment income")
                .active(true)
                .build();

        when(spendCategoryRepository.existsByNameIgnoreCase("Salary")).thenReturn(false);
        when(spendCategoryMapper.toEntity(requestDTO)).thenReturn(category);
        when(spendCategoryRepository.save(any(SpendCategory.class))).thenReturn(category);
        when(spendCategoryMapper.toResponseDTO(category)).thenReturn(response);

        // act
        SpendCategoryResponseDTO result = spendCategoryService.create(requestDTO);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Salary");
        verify(spendCategoryRepository).existsByNameIgnoreCase("Salary");
        verify(spendCategoryMapper).toEntity(requestDTO);
        verify(spendCategoryRepository).save(any(SpendCategory.class));
    }

    @Test
    void shouldThrowException_whenDuplicateName() {
        // arrange
        SpendCategoryRequestDTO requestDTO = SpendCategoryRequestDTO.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .build();

        when(spendCategoryRepository.existsByNameIgnoreCase("Salary")).thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> spendCategoryService.create(requestDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(spendCategoryRepository).existsByNameIgnoreCase("Salary");
        verify(spendCategoryRepository, never()).save(any());
    }

    @Test
    void shouldUpdateCategory_whenValidRequest() {
        // arrange
        SpendCategoryRequestDTO requestDTO = SpendCategoryRequestDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        SpendCategory category = SpendCategory.builder()
                .id(1L)
                .name("Original Name")
                .type(SpendCategoryType.INFLOW)
                .active(true)
                .build();

        SpendCategory updatedCategory = SpendCategory.builder()
                .id(1L)
                .name("Updated Name")
                .type(SpendCategoryType.INFLOW)
                .description("Updated description")
                .active(true)
                .build();

        SpendCategoryResponseDTO response = SpendCategoryResponseDTO.builder()
                .id(1L)
                .name("Updated Name")
                .build();

        when(spendCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(spendCategoryRepository.existsByNameIgnoreCase("Updated Name")).thenReturn(false);
        when(spendCategoryRepository.save(any(SpendCategory.class))).thenReturn(updatedCategory);
        when(spendCategoryMapper.toResponseDTO(updatedCategory)).thenReturn(response);

        // act
        SpendCategoryResponseDTO result = spendCategoryService.update(1L, requestDTO);

        // assert
        assertThat(result).isNotNull();
        verify(spendCategoryRepository).findById(1L);
        verify(spendCategoryMapper).updateEntityFromDTO(requestDTO, category);
        verify(spendCategoryRepository).save(any(SpendCategory.class));
    }

    @Test
    void shouldDeactivateCategory_whenValidId() {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .id(1L)
                .name("Salary")
                .active(true)
                .build();

        when(spendCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(spendCategoryRepository.save(any(SpendCategory.class))).thenReturn(category);

        // act
        spendCategoryService.deactivate(1L);

        // assert
        verify(spendCategoryRepository).findById(1L);
        verify(spendCategoryRepository).save(category);
        assertThat(category.isActive()).isFalse();
    }
}

