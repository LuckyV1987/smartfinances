package com.smartfinances.repository;

import com.smartfinances.entity.TransactionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, Long> {

    // Only ever accessed via transaction ID — no independent queries
    List<TransactionDetail> findByTransactionId(Long transactionId);

    void deleteByTransactionId(Long transactionId);
}

