package com.smartfinances.repository;

import com.smartfinances.entity.SpendCategory;
import com.smartfinances.entity.enums.SpendCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpendCategoryRepository extends JpaRepository<SpendCategory, Long> {

    List<SpendCategory> findByActiveTrue();

    Optional<SpendCategory> findByIdAndActiveTrue(Long id);

    List<SpendCategory> findByTypeAndActiveTrue(SpendCategoryType type);

    Optional<SpendCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}

