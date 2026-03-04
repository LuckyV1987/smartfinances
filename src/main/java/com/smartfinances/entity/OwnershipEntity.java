package com.smartfinances.entity;

import com.smartfinances.entity.enums.OwnershipEntityType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "ownership_entities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OwnershipEntityType type;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OwnershipEntity)) return false;
        OwnershipEntity that = (OwnershipEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

