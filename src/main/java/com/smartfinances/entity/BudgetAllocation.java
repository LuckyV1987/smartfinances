package com.smartfinances.entity;

import com.smartfinances.entity.enums.AllocationIntervalEnum;
import com.smartfinances.entity.enums.AllocationTypeEnum;
import com.smartfinances.entity.enums.AllocationUnitEnum;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "budget_allocations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_budget_allocation_entity_name",
            columnNames = {"ownership_entity_id", "name"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BudgetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownership_entity_id", nullable = false)
    private OwnershipEntity ownershipEntity;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AllocationTypeEnum type;

    // Spending limit, savings target, or expected inflow.
    // Interpreted per targetUnit. Null = no target set.
    @Column(name = "target_amount", precision = 19, scale = 4)
    private BigDecimal targetAmount;

    // Required when targetAmount is set.
    @Enumerated(EnumType.STRING)
    @Column(name = "target_unit", length = 20)
    private AllocationUnitEnum targetUnit;

    // Null = one-time allocation (no recurrence)
    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_interval", length = 20)
    private AllocationIntervalEnum interval;

    // Only applicable for SINKING_FUND
    @Column(name = "target_date")
    private LocalDate targetDate;

    // If true, a system job creates a carryover transaction at interval start.
    // Balance calculation is always: sum of transactions within current interval.
    // The carryover transaction is treated as any other transaction.
    @Column(nullable = false)
    @Builder.Default
    private boolean rollover = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BudgetAllocation)) return false;
        BudgetAllocation that = (BudgetAllocation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

