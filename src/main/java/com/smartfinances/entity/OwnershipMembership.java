package com.smartfinances.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
    name = "ownership_memberships",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_ownership_membership_user_entity",
            columnNames = {"user_id", "ownership_entity_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipMembership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownership_entity_id", nullable = false)
    private OwnershipEntity ownershipEntity;

    @Column(nullable = false)
    private boolean active = true;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OwnershipMembership)) return false;
        OwnershipMembership that = (OwnershipMembership) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

