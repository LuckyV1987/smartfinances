package com.smartfinances.entity;

import com.smartfinances.entity.enums.TransactionTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownership_entity_id", nullable = false)
    private OwnershipEntity ownershipEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_account_id", nullable = false)
    private FinancialAccount financialAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spend_category_id", nullable = false)
    private SpendCategory spendCategory;

    // Optional — links this transaction to a budget allocation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_allocation_id")
    private BudgetAllocation budgetAllocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionTypeEnum type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // The date the transaction occurred (user-provided, not system date)
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(length = 255)
    private String description;

    // Reference number, receipt number, or external ID
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    // CARRYOVER transactions are system-generated — flag for identification
    @Column(name = "system_generated", nullable = false)
    @Builder.Default
    private boolean systemGenerated = false;

    // No updatedAt — transactions are immutable after creation
    // Note: BaseEntity provides updatedAt but it won't be used (acceptable per spec)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

