package com.smartfinances.repository;

import com.smartfinances.entity.BudgetAllocationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BudgetAllocationCategoryRepository
        extends JpaRepository<BudgetAllocationCategory, Long> {

    List<BudgetAllocationCategory> findByBudgetAllocationId(Long budgetAllocationId);

    boolean existsByBudgetAllocationIdAndSpendCategoryId(
        Long budgetAllocationId, Long spendCategoryId
    );

    @Transactional
    void deleteByBudgetAllocationIdAndSpendCategoryId(
        Long budgetAllocationId, Long spendCategoryId
    );
}

