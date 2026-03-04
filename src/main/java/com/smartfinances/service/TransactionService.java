package com.smartfinances.service;

import com.smartfinances.dto.request.TransactionRequestDTO;
import com.smartfinances.dto.response.TransactionResponseDTO;
import com.smartfinances.entity.*;
import com.smartfinances.entity.enums.TransactionTypeEnum;
import com.smartfinances.exception.InvalidRequestException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.TransactionMapper;
import com.smartfinances.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OwnershipEntityRepository ownershipEntityRepository;
    private final FinancialAccountRepository financialAccountRepository;
    private final SpendCategoryRepository spendCategoryRepository;
    private final BudgetAllocationRepository budgetAllocationRepository;
    private final TransactionMapper transactionMapper;

    public TransactionService(
            TransactionRepository transactionRepository,
            OwnershipEntityRepository ownershipEntityRepository,
            FinancialAccountRepository financialAccountRepository,
            SpendCategoryRepository spendCategoryRepository,
            BudgetAllocationRepository budgetAllocationRepository,
            TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.ownershipEntityRepository = ownershipEntityRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.spendCategoryRepository = spendCategoryRepository;
        this.budgetAllocationRepository = budgetAllocationRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> findAllByEntity(Long ownershipEntityId) {
        // Verify entity exists and is active
        ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        List<Transaction> transactions = transactionRepository.findByOwnershipEntityIdOrderByTransactionDateDesc(ownershipEntityId);
        return transactions.stream()
                .map(transactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> findAllByAccount(Long financialAccountId) {
        // Verify account exists and is active
        financialAccountRepository.findByIdAndActiveTrue(financialAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found with id: " + financialAccountId));

        List<Transaction> transactions = transactionRepository.findByFinancialAccountIdOrderByTransactionDateDesc(financialAccountId);
        return transactions.stream()
                .map(transactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> findAllByAllocation(Long budgetAllocationId) {
        // Verify allocation exists and is active
        budgetAllocationRepository.findByIdAndActiveTrue(budgetAllocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + budgetAllocationId));

        List<Transaction> transactions = transactionRepository.findByBudgetAllocationIdOrderByTransactionDateDesc(budgetAllocationId);
        return transactions.stream()
                .map(transactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return transactionMapper.toResponseDTO(transaction);
    }

    @Transactional
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        // Reject CARRYOVER type - system-generated only
        if (dto.getType() == TransactionTypeEnum.CARRYOVER) {
            throw new InvalidRequestException("CARRYOVER transactions are system-generated only and cannot be created via API");
        }

        // Validate ownership entity exists and active
        OwnershipEntity ownershipEntity = ownershipEntityRepository.findByIdAndActiveTrue(dto.getOwnershipEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + dto.getOwnershipEntityId()));

        // Validate financial account exists and active
        FinancialAccount financialAccount = financialAccountRepository.findByIdAndActiveTrue(dto.getFinancialAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found with id: " + dto.getFinancialAccountId()));

        // Validate spend category exists and active
        SpendCategory spendCategory = spendCategoryRepository.findByIdAndActiveTrue(dto.getSpendCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Spend category not found with id: " + dto.getSpendCategoryId()));

        // Validate budget allocation exists and active if provided
        BudgetAllocation budgetAllocation = null;
        if (dto.getBudgetAllocationId() != null) {
            budgetAllocation = budgetAllocationRepository.findByIdAndActiveTrue(dto.getBudgetAllocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Budget allocation not found with id: " + dto.getBudgetAllocationId()));
        }

        // Create entity
        Transaction transaction = transactionMapper.toEntity(dto);
        transaction.setOwnershipEntity(ownershipEntity);
        transaction.setFinancialAccount(financialAccount);
        transaction.setSpendCategory(spendCategory);
        transaction.setBudgetAllocation(budgetAllocation);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponseDTO(savedTransaction);
    }

    // NOTE ON IMMUTABILITY:
    // Transactions have no update or delete methods.
    // Corrections are made by creating a new ADJUSTMENT transaction.
    // No deactivate method exists.
    // No updatedAt field exists on the entity.
}

