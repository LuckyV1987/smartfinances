package com.smartfinances.repository;

import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.enums.OwnershipEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnershipEntityRepository extends JpaRepository<OwnershipEntity, Long> {

    List<OwnershipEntity> findByActiveTrue();

    Optional<OwnershipEntity> findByIdAndActiveTrue(Long id);

    List<OwnershipEntity> findByTypeAndActiveTrue(OwnershipEntityType type);
}

