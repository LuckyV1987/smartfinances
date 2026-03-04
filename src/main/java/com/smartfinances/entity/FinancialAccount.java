package com.smartfinances.entity;

import com.smartfinances.entity.enums.AccountTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
    name = "financial_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_financial_account_entity_name",
            columnNames = {"ownership_entity_id", "name"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialAccount extends BaseEntity {

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
    @Column(nullable = false, length = 20)
    private AccountTypeEnum type;

    // ISO 4217 currency code e.g. USD, AUD.
    // Single currency per account for now.
    // Stored here so multi-currency support requires no schema change.
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinancialAccount)) return false;
        FinancialAccount that = (FinancialAccount) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

