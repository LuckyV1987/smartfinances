package com.smartfinances.service;

import com.smartfinances.dto.request.BudgetAllocationRequestDTO;
import com.smartfinances.dto.request.BudgetAllocationUpdateRequestDTO;
import com.smartfinances.dto.response.BudgetAllocationResponseDTO;
import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.BudgetAllocation;
import com.smartfinances.entity.OwnershipEntity;
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
import com.smartfinances.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetAllocationService {

    private final BudgetAllocationRepository budgetAllocationRepository;
    private final OwnershipEntityRepository ownershipEntityRepository;
    private final BudgetAllocationCategoryRepository budgetAllocationCategoryRepository;
    private final SpendCategoryRepository spendCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetAllocationMapper budgetAllocationMapper;
    private final SpendCategoryMapper spendCategoryMapper;

    public BudgetAllocationService(
            BudgetAllocationRepository budgetAllocationRepository,
            OwnershipEntityRepository ownershipEntityRepository,
            BudgetAllocationCategoryRepository budgetAllocationCategoryRepository,
            SpendCategoryRepository spendCategoryRepository,
            TransactionRepository transactionRepository,
            BudgetAllocationMapper budgetAllocationMapper,
            SpendCategoryMapper spendCategoryMapper) {
        this.budgetAllocationRepository = budgetAllocationRepository;
        this.ownershipEntityRepository = ownershipEntityRepository;
        this.budgetAllocationCategoryRepository = budgetAllocationCategoryRepository;
        this.spendCategoryRepository = spendCategoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetAllocationMapper = budgetAllocationMapper;
        this.spendCategoryMapper = spendCategoryMapper;
    }

    @Transactional(readOnly = true)
    public List<BudgetAllocationResponseDTO> findAllByEntity(Long ownershipEntityId) {
        // Verify entity exists and is active
        ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        List<BudgetAllocation> allocations = budgetAllocationRepository.findByOwnershipEntityIdAndActiveTrue(ownershipEntityId);
        return allocations.stream()
                .map(this::buildResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetAllocationResponseDTO> findAllByEntityAndType(Long ownershipEntityId, AllocationTypeEnum type) {
        // Verify entity exists and is active
        ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        List<BudgetAllocation> allocations = budgetAllocationRepository.findByOwnershipEntityIdAndTypeAndActiveTrue(ownershipEntityId, type);
        return allocations.stream()
                .map(this::buildResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetAllocationResponseDTO findById(Long id) {
        BudgetAllocation allocation = budgetAllocationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + id));
        return buildResponseDTO(allocation);
    }

    @Transactional(readOnly = true)
    public BudgetAllocationResponseDTO checkBalance(Long id) {
        // Forces fresh balance calculation
        BudgetAllocation allocation = budgetAllocationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + id));
        return buildResponseDTO(allocation);
    }

    @Transactional
    public BudgetAllocationResponseDTO create(BudgetAllocationRequestDTO dto) {
        // Validate ownership entity exists and active
        OwnershipEntity ownershipEntity = ownershipEntityRepository.findByIdAndActiveTrue(dto.getOwnershipEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + dto.getOwnershipEntityId()));

        // Validate name uniqueness per entity
        if (budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(dto.getOwnershipEntityId(), dto.getName())) {
            throw new DuplicateResourceException("Budget allocation with name '" + dto.getName() + "' already exists for this ownership entity");
        }

        // Apply validation rules
        validateAllocationRules(dto);

        // Create entity
        BudgetAllocation allocation = budgetAllocationMapper.toEntity(dto);
        allocation.setOwnershipEntity(ownershipEntity);

        BudgetAllocation savedAllocation = budgetAllocationRepository.save(allocation);

        // Return with empty categories list and zero balance
        return budgetAllocationMapper.toResponseDTO(savedAllocation, List.of(), BigDecimal.ZERO);
    }

    @Transactional
    public BudgetAllocationResponseDTO update(Long id, BudgetAllocationUpdateRequestDTO dto) {
        BudgetAllocation allocation = budgetAllocationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + id));

        // Validate name uniqueness if changed
        if (dto.getName() != null && !dto.getName().equalsIgnoreCase(allocation.getName())) {
            if (budgetAllocationRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(
                    allocation.getOwnershipEntity().getId(), dto.getName())) {
                throw new DuplicateResourceException("Budget allocation with name '" + dto.getName() + "' already exists for this ownership entity");
            }
        }

        // Note: We don't validate rules on update since fields are optional
        // Validation happens on create with the full DTO

        // Update entity (non-null fields only, type and ownershipEntityId are immutable)
        budgetAllocationMapper.updateEntityFromDTO(dto, allocation);

        BudgetAllocation updatedAllocation = budgetAllocationRepository.save(allocation);
        return buildResponseDTO(updatedAllocation);
    }

    @Transactional
    public void deactivate(Long id) {
        BudgetAllocation allocation = budgetAllocationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + id));

        allocation.setActive(false);
        budgetAllocationRepository.save(allocation);
    }

    // Helper method to build response DTO with categories and balance
    private BudgetAllocationResponseDTO buildResponseDTO(BudgetAllocation allocation) {
        // Get linked categories
        List<SpendCategoryResponseDTO> categories = budgetAllocationCategoryRepository
                .findByBudgetAllocationId(allocation.getId())
                .stream()
                .map(bac -> spendCategoryMapper.toResponseDTO(bac.getSpendCategory()))
                .collect(Collectors.toList());

        // Calculate balance (stub implementation - always returns ZERO until transactions are implemented)
        BigDecimal currentBalance = calculateBalance(allocation);

        return budgetAllocationMapper.toResponseDTO(allocation, categories, currentBalance);
    }

    // Balance calculation - sums transactions by allocation within current interval
    private BigDecimal calculateBalance(BudgetAllocation allocation) {
        // interval is null (one-time): sum ALL linked transactions ever
        if (allocation.getInterval() == null) {
            return transactionRepository.sumAmountByAllocationId(allocation.getId());
        }

        // interval is set: sum transactions within current period
        // Calculate current period based on interval
        LocalDate startDate = calculateIntervalStartDate(allocation.getInterval());
        LocalDate endDate = LocalDate.now();

        return transactionRepository.sumAmountByAllocationIdAndDateRange(
                allocation.getId(), startDate, endDate);
    }

    // Helper to calculate the start date of the current interval period
    private LocalDate calculateIntervalStartDate(com.smartfinances.entity.enums.AllocationIntervalEnum interval) {
        LocalDate now = LocalDate.now();

        return switch (interval) {
            case WEEKLY -> now.minusWeeks(1).plusDays(1); // Start of current week
            case FORTNIGHTLY -> now.minusWeeks(2).plusDays(1);
            case MONTHLY -> now.withDayOfMonth(1); // 1st of current month
            case QUARTERLY -> now.withDayOfMonth(1).minusMonths(now.getMonthValue() % 3);
            case YEARLY -> now.withDayOfYear(1); // Jan 1st of current year
        };
    }

    // Validation rules
    private void validateAllocationRules(BudgetAllocationRequestDTO dto) {
        // SINKING_FUND requires targetDate
        if (dto.getType() == AllocationTypeEnum.SINKING_FUND && dto.getTargetDate() == null) {
            throw new InvalidRequestException("SINKING_FUND type requires a target date");
        }

        // rollover=true requires interval
        if (dto.isRollover() && dto.getInterval() == null) {
            throw new InvalidRequestException("Rollover requires an interval to be set");
        }

        // targetAmount provided requires targetUnit
        if (dto.getTargetAmount() != null && dto.getTargetUnit() == null) {
            throw new InvalidRequestException("Target amount requires a target unit (FIXED or PERCENTAGE)");
        }

        // PERCENTAGE unit: value must be between 0.0001 and 1.0000
        if (dto.getTargetUnit() == AllocationUnitEnum.PERCENTAGE && dto.getTargetAmount() != null) {
            if (dto.getTargetAmount().compareTo(new BigDecimal("0.0001")) < 0 ||
                dto.getTargetAmount().compareTo(BigDecimal.ONE) > 0) {
                throw new InvalidRequestException("PERCENTAGE unit value must be between 0.0001 and 1.0000");
            }
        }

        // FIXED unit: value must be greater than 0
        if (dto.getTargetUnit() == AllocationUnitEnum.FIXED && dto.getTargetAmount() != null) {
            if (dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidRequestException("FIXED unit value must be greater than 0");
            }
        }
    }
}


