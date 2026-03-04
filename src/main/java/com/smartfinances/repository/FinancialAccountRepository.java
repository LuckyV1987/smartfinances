package com.smartfinances.repository;

import com.smartfinances.entity.FinancialAccount;
import com.smartfinances.entity.enums.AccountTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {

    List<FinancialAccount> findByOwnershipEntityIdAndActiveTrue(Long ownershipEntityId);

    Optional<FinancialAccount> findByIdAndActiveTrue(Long id);

    List<FinancialAccount> findByOwnershipEntityIdAndTypeAndActiveTrue(
        Long ownershipEntityId, AccountTypeEnum type
    );

    boolean existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(
        Long ownershipEntityId, String name
    );
}

