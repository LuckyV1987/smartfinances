package com.smartfinances.entity;

import com.smartfinances.entity.enums.SpendCategoryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "spend_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpendCategoryType type;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}

