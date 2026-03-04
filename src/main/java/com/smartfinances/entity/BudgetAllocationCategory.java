package com.smartfinances.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "budget_allocation_categories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_bac_allocation_category",
            columnNames = {"budget_allocation_id", "spend_category_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BudgetAllocationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_allocation_id", nullable = false)
    private BudgetAllocation budgetAllocation;

    // System SpendCategory only.
    // Future user-defined categories use a SEPARATE join table.
    // Do NOT add user_spend_category_id to this table.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spend_category_id", nullable = false)
    private SpendCategory spendCategory;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BudgetAllocationCategory)) return false;
        BudgetAllocationCategory that = (BudgetAllocationCategory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

