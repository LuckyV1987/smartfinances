package com.smartfinances.repository;

import com.smartfinances.entity.BudgetAllocation;
import com.smartfinances.entity.enums.AllocationTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetAllocationRepository extends JpaRepository<BudgetAllocation, Long> {

    List<BudgetAllocation> findByOwnershipEntityIdAndActiveTrue(Long ownershipEntityId);

    Optional<BudgetAllocation> findByIdAndActiveTrue(Long id);

    List<BudgetAllocation> findByOwnershipEntityIdAndTypeAndActiveTrue(
        Long ownershipEntityId, AllocationTypeEnum type
    );

    boolean existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(
        Long ownershipEntityId, String name
    );

    // Hook for the future rollover job
    List<BudgetAllocation> findByRolloverTrueAndActiveTrueAndIntervalIsNotNull();
}

