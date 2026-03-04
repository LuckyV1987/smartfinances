package com.smartfinances.repository;

import com.smartfinances.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByOwnershipEntityIdOrderByTransactionDateDesc(Long ownershipEntityId);

    List<Transaction> findByFinancialAccountIdOrderByTransactionDateDesc(Long financialAccountId);

    List<Transaction> findByBudgetAllocationIdOrderByTransactionDateDesc(Long budgetAllocationId);

    List<Transaction> findBySpendCategoryIdOrderByTransactionDateDesc(Long spendCategoryId);

    // For balance calculation on FinancialAccount
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.financialAccount.id = :accountId")
    BigDecimal sumAmountByFinancialAccountId(@Param("accountId") Long accountId);

    // For balance calculation on BudgetAllocation within an interval
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.budgetAllocation.id = :allocationId " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumAmountByAllocationIdAndDateRange(
        @Param("allocationId") Long allocationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // For balance calculation on BudgetAllocation with no interval (one-time)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.budgetAllocation.id = :allocationId")
    BigDecimal sumAmountByAllocationId(@Param("allocationId") Long allocationId);
}

