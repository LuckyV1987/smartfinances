package com.smartfinances.service;

import com.smartfinances.dto.request.BudgetAllocationRequestDTO;
import com.smartfinances.dto.request.BudgetAllocationUpdateRequestDTO;
import com.smartfinances.dto.response.BudgetAllocationResponseDTO;
import com.smartfinances.entity.BudgetAllocation;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.enums.AllocationIntervalEnum;
import com.smartfinances.entity.enums.AllocationTypeEnum;
import com.smartfinances.entity.enums.AllocationUnitEnum;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.InvalidRequestException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.BudgetAllocationMapper;
import com.smartfinances.mapper.SpendCategoryMapper;
import com.smartfinances.repository.BudgetAllocationCategoryRepository;
import com.smartfinances.repository.BudgetAllocationRepository;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.SpendCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetAllocationServiceTest {

    @Mock
    private BudgetAllocationRepository budgetAllocationRepository;

    @Mock
    private OwnershipEntityRepository ownershipEntityRepository;

    @Mock
    private BudgetAllocationCategoryRepository budgetAllocationCategoryRepository;

    @Mock
    private SpendCategoryRepository spendCategoryRepository;

    @Mock
    private BudgetAllocationMapper budgetAllocationMapper;

    @Mock
    private SpendCategoryMapper spendCategoryMapper;

    @InjectMocks
    private BudgetAllocationService budgetAllocationService;

    @Test
    void shouldReturnAllocations_whenFindAllByEntity() {
        // arrange
        Long entityId = 1L;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Test Entity")
                .active(true)
                .build();

        BudgetAllocation allocation1 = BudgetAllocation.builder()
                .id(1L)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .build();

        BudgetAllocation allocation2 = BudgetAllocation.builder()
                .id(2L)
                .name("Emergency Fund")
                .type(AllocationTypeEnum.SAVINGS)
                .build();

        BudgetAllocationResponseDTO dto1 = BudgetAllocationResponseDTO.builder()
                .id(1L)
                .name("Groceries")
                .build();

        BudgetAllocationResponseDTO dto2 = BudgetAllocationResponseDTO.builder()
                .id(2L)
                .name("Emergency Fund")
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.findByOwnershipEntityIdAndActiveTrue(entityId))
                .thenReturn(Arrays.asList(allocation1, allocation2));
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(1L)).thenReturn(List.of());
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(2L)).thenReturn(List.of());
        when(budgetAllocationMapper.toResponseDTO(eq(allocation1), any(), any())).thenReturn(dto1);
        when(budgetAllocationMapper.toResponseDTO(eq(allocation2), any(), any())).thenReturn(dto2);

        // act
        List<BudgetAllocationResponseDTO> result = budgetAllocationService.findAllByEntity(entityId);

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Groceries");
        assertThat(result.get(1).getName()).isEqualTo("Emergency Fund");
        verify(ownershipEntityRepository).findByIdAndActiveTrue(entityId);
        verify(budgetAllocationRepository).findByOwnershipEntityIdAndActiveTrue(entityId);
    }

    @Test
    void shouldReturnAllocationsByType_whenFindAllByEntityAndType() {
        // arrange
        Long entityId = 1L;
        AllocationTypeEnum type = AllocationTypeEnum.BUDGET;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .active(true)
                .build();

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(1L)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .build();

        BudgetAllocationResponseDTO dto = BudgetAllocationResponseDTO.builder()
                .id(1L)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.findByOwnershipEntityIdAndTypeAndActiveTrue(entityId, type))
                .thenReturn(List.of(allocation));
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(1L)).thenReturn(List.of());
        when(budgetAllocationMapper.toResponseDTO(eq(allocation), any(), any())).thenReturn(dto);

        // act
        List<BudgetAllocationResponseDTO> result = budgetAllocationService.findAllByEntityAndType(entityId, type);

        // assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(AllocationTypeEnum.BUDGET);
    }

    @Test
    void shouldReturnAllocation_whenValidId() {
        // arrange
        Long allocationId = 1L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .name("Groceries")
                .active(true)
                .build();

        BudgetAllocationResponseDTO dto = BudgetAllocationResponseDTO.builder()
                .id(allocationId)
                .name("Groceries")
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId)).thenReturn(List.of());
        when(budgetAllocationMapper.toResponseDTO(eq(allocation), any(), any())).thenReturn(dto);

        // act
        BudgetAllocationResponseDTO result = budgetAllocationService.findById(allocationId);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Groceries");
        verify(budgetAllocationRepository).findByIdAndActiveTrue(allocationId);
    }

    @Test
    void shouldThrowException_whenAllocationNotFound() {
        // arrange
        Long allocationId = 99L;
        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> budgetAllocationService.findById(allocationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget allocation not found");
    }

    @Test
    void shouldThrowException_whenAllocationInactive() {
        // arrange
        Long allocationId = 1L;
        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> budgetAllocationService.findById(allocationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget allocation not found");
    }

    @Test
    void shouldReturnZeroBalance_whenNoTransactionsLinked() {
        // arrange
        Long allocationId = 1L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .name("Groceries")
                .active(true)
                .build();

        BudgetAllocationResponseDTO dto = BudgetAllocationResponseDTO.builder()
                .id(allocationId)
                .currentBalance(BigDecimal.ZERO)
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId)).thenReturn(List.of());
        when(budgetAllocationMapper.toResponseDTO(eq(allocation), any(), eq(BigDecimal.ZERO))).thenReturn(dto);

        // act
        BudgetAllocationResponseDTO result = budgetAllocationService.checkBalance(allocationId);

        // assert
        assertThat(result.getCurrentBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCreateAllocation_whenValidFixedRequest() {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .targetAmount(new BigDecimal("500.00"))
                .targetUnit(AllocationUnitEnum.FIXED)
                .interval(AllocationIntervalEnum.MONTHLY)
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        BudgetAllocation allocation = BudgetAllocation.builder()
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .build();

        BudgetAllocation savedAllocation = BudgetAllocation.builder()
                .id(1L)
                .name("Groceries")
                .build();

        BudgetAllocationResponseDTO response = BudgetAllocationResponseDTO.builder()
                .id(1L)
                .name("Groceries")
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Groceries"))
                .thenReturn(false);
        when(budgetAllocationMapper.toEntity(request)).thenReturn(allocation);
        when(budgetAllocationRepository.save(allocation)).thenReturn(savedAllocation);
        when(budgetAllocationMapper.toResponseDTO(eq(savedAllocation), eq(List.of()), eq(BigDecimal.ZERO)))
                .thenReturn(response);

        // act
        BudgetAllocationResponseDTO result = budgetAllocationService.create(request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Groceries");
        verify(budgetAllocationRepository).save(allocation);
    }

    @Test
    void shouldCreateAllocation_whenValidPercentageRequest() {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Savings")
                .type(AllocationTypeEnum.SAVINGS)
                .targetAmount(new BigDecimal("0.20"))
                .targetUnit(AllocationUnitEnum.PERCENTAGE)
                .interval(AllocationIntervalEnum.MONTHLY)
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        BudgetAllocation allocation = BudgetAllocation.builder().build();
        BudgetAllocation savedAllocation = BudgetAllocation.builder().id(1L).build();
        BudgetAllocationResponseDTO response = BudgetAllocationResponseDTO.builder().id(1L).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Savings"))
                .thenReturn(false);
        when(budgetAllocationMapper.toEntity(request)).thenReturn(allocation);
        when(budgetAllocationRepository.save(allocation)).thenReturn(savedAllocation);
        when(budgetAllocationMapper.toResponseDTO(eq(savedAllocation), any(), any())).thenReturn(response);

        // act
        BudgetAllocationResponseDTO result = budgetAllocationService.create(request);

        // assert
        assertThat(result).isNotNull();
        verify(budgetAllocationRepository).save(allocation);
    }

    @Test
    void shouldCreateOneTimeAllocation_whenNoInterval() {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Bonus")
                .type(AllocationTypeEnum.INCOME)
                .interval(null) // one-time
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        BudgetAllocation allocation = BudgetAllocation.builder().build();
        BudgetAllocation savedAllocation = BudgetAllocation.builder().id(1L).build();
        BudgetAllocationResponseDTO response = BudgetAllocationResponseDTO.builder().id(1L).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Bonus"))
                .thenReturn(false);
        when(budgetAllocationMapper.toEntity(request)).thenReturn(allocation);
        when(budgetAllocationRepository.save(allocation)).thenReturn(savedAllocation);
        when(budgetAllocationMapper.toResponseDTO(eq(savedAllocation), any(), any())).thenReturn(response);

        // act
        BudgetAllocationResponseDTO result = budgetAllocationService.create(request);

        // assert
        assertThat(result).isNotNull();
        verify(budgetAllocationRepository).save(allocation);
    }

    @Test
    void shouldThrowException_whenSinkingFundHasNoTargetDate() {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Vacation")
                .type(AllocationTypeEnum.SINKING_FUND)
                .targetDate(null) // Missing required field
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Vacation"))
                .thenReturn(false);

        // act & assert
        assertThatThrownBy(() -> budgetAllocationService.create(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("SINKING_FUND type requires a target date");
    }

    @Test
    void shouldThrowException_whenRolloverWithNoInterval() {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .rollover(true)
                .interval(null) // Missing required field
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Groceries"))
                .thenReturn(false);

        // act & assert
        assertThatThrownBy(() -> budgetAllocationService.create(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Rollover requires an interval");
    }

    @Test
    void shouldThrowException_whenTargetAmountHasNoUnit() {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .targetAmount(new BigDecimal("500.00"))
                .targetUnit(null) // Missing required field
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Groceries"))
                .thenReturn(false);

        // act & assert
        assertThatThrownBy(() -> budgetAllocationService.create(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Target amount requires a target unit");
    }

    @Test
    void shouldThrowException_whenDuplicateNameForEntity() {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Groceries"))
                .thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> budgetAllocationService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldUpdateAllocation_whenValidRequest() {
        // arrange
        Long allocationId = 1L;
        BudgetAllocationUpdateRequestDTO request = BudgetAllocationUpdateRequestDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .build();

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .name("Old Name")
                .ownershipEntity(entity)
                .active(true)
                .build();

        BudgetAllocation updatedAllocation = BudgetAllocation.builder()
                .id(allocationId)
                .name("Updated Name")
                .build();

        BudgetAllocationResponseDTO response = BudgetAllocationResponseDTO.builder()
                .id(allocationId)
                .name("Updated Name")
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Updated Name"))
                .thenReturn(false);
        doNothing().when(budgetAllocationMapper).updateEntityFromDTO(request, allocation);
        when(budgetAllocationRepository.save(allocation)).thenReturn(updatedAllocation);
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId)).thenReturn(List.of());
        when(budgetAllocationMapper.toResponseDTO(eq(updatedAllocation), any(), any())).thenReturn(response);

        // act
        BudgetAllocationResponseDTO result = budgetAllocationService.update(allocationId, request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(budgetAllocationMapper).updateEntityFromDTO(request, allocation);
        verify(budgetAllocationRepository).save(allocation);
    }

    @Test
    void shouldNotUpdateType_whenTypeProvidedInUpdate() {
        // arrange - This test verifies that type is immutable
        // The mapper should never update the type field
        // Note: Update DTO doesn't even have a type field, so this test just verifies
        // that type stays unchanged after any update
        Long allocationId = 1L;
        BudgetAllocationUpdateRequestDTO request = BudgetAllocationUpdateRequestDTO.builder()
                .name("Updated Name")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .build();

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .name("Old Name")
                .type(AllocationTypeEnum.BUDGET) // Original type
                .ownershipEntity(entity)
                .active(true)
                .build();

        BudgetAllocation savedAllocation = BudgetAllocation.builder()
                .id(allocationId)
                .type(AllocationTypeEnum.BUDGET) // Type unchanged
                .build();

        BudgetAllocationResponseDTO response = BudgetAllocationResponseDTO.builder()
                .id(allocationId)
                .type(AllocationTypeEnum.BUDGET)
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Updated Name"))
                .thenReturn(false);
        doNothing().when(budgetAllocationMapper).updateEntityFromDTO(request, allocation);
        when(budgetAllocationRepository.save(allocation)).thenReturn(savedAllocation);
        when(budgetAllocationCategoryRepository.findByBudgetAllocationId(allocationId)).thenReturn(List.of());
        when(budgetAllocationMapper.toResponseDTO(eq(savedAllocation), any(), any())).thenReturn(response);

        // act
        BudgetAllocationResponseDTO result = budgetAllocationService.update(allocationId, request);

        // assert
        assertThat(result.getType()).isEqualTo(AllocationTypeEnum.BUDGET);
        verify(budgetAllocationMapper).updateEntityFromDTO(request, allocation);
    }

    @Test
    void shouldDeactivateAllocation_whenValidId() {
        // arrange
        Long allocationId = 1L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .active(true)
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(budgetAllocationRepository.save(allocation)).thenReturn(allocation);

        // act
        budgetAllocationService.deactivate(allocationId);

        // assert
        assertThat(allocation.isActive()).isFalse();
        verify(budgetAllocationRepository).save(allocation);
    }
}


