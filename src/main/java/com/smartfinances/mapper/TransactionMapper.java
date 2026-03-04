package com.smartfinances.mapper;

import com.smartfinances.dto.request.TransactionDetailRequestDTO;
import com.smartfinances.dto.request.TransactionRequestDTO;
import com.smartfinances.dto.response.TransactionDetailResponseDTO;
import com.smartfinances.dto.response.TransactionResponseDTO;
import com.smartfinances.entity.Transaction;
import com.smartfinances.entity.TransactionDetail;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    /**
     * Maps transaction entity to response DTO.
     * Includes spend category name for convenience.
     */
    public TransactionResponseDTO toResponseDTO(Transaction entity) {
        return TransactionResponseDTO.builder()
                .id(entity.getId())
                .ownershipEntityId(entity.getOwnershipEntity().getId())
                .financialAccountId(entity.getFinancialAccount().getId())
                .spendCategoryId(entity.getSpendCategory().getId())
                .spendCategoryName(entity.getSpendCategory().getName())
                .budgetAllocationId(entity.getBudgetAllocation() != null ? entity.getBudgetAllocation().getId() : null)
                .type(entity.getType())
                .amount(entity.getAmount())
                .transactionDate(entity.getTransactionDate())
                .description(entity.getDescription())
                .referenceNumber(entity.getReferenceNumber())
                .systemGenerated(entity.isSystemGenerated())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Maps request DTO to new transaction entity.
     * Does not set ownershipEntity, financialAccount, spendCategory, budgetAllocation.
     * These are resolved in service via repository lookups.
     * Never sets systemGenerated = true — only the rollover job may do that.
     */
    public Transaction toEntity(TransactionRequestDTO dto) {
        return Transaction.builder()
                .type(dto.getType())
                .amount(dto.getAmount())
                .transactionDate(dto.getTransactionDate())
                .description(dto.getDescription())
                .referenceNumber(dto.getReferenceNumber())
                .systemGenerated(false) // Always false for API-created transactions
                .build();
    }

    /**
     * Maps transaction detail entity to response DTO.
     */
    public TransactionDetailResponseDTO toDetailResponseDTO(TransactionDetail detail) {
        return TransactionDetailResponseDTO.builder()
                .id(detail.getId())
                .transactionId(detail.getTransaction().getId())
                .name(detail.getName())
                .quantity(detail.getQuantity())
                .unit(detail.getUnit())
                .unitPrice(detail.getUnitPrice())
                .totalPrice(detail.getTotalPrice())
                .notes(detail.getNotes())
                .createdAt(detail.getCreatedAt())
                .build();
    }

    /**
     * Maps detail request DTO to new detail entity.
     * Sets the transaction relationship.
     */
    public TransactionDetail toDetailEntity(TransactionDetailRequestDTO dto, Transaction transaction) {
        return TransactionDetail.builder()
                .transaction(transaction)
                .name(dto.getName())
                .quantity(dto.getQuantity())
                .unit(dto.getUnit())
                .unitPrice(dto.getUnitPrice())
                .totalPrice(dto.getTotalPrice())
                .notes(dto.getNotes())
                .build();
    }
}

