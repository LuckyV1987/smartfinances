package com.smartfinances.service;

import com.smartfinances.dto.request.TransactionRequestDTO;
import com.smartfinances.dto.response.TransactionResponseDTO;
import com.smartfinances.entity.*;
import com.smartfinances.entity.enums.TransactionTypeEnum;
import com.smartfinances.exception.InvalidRequestException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.TransactionMapper;
import com.smartfinances.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OwnershipEntityRepository ownershipEntityRepository;

    @Mock
    private FinancialAccountRepository financialAccountRepository;

    @Mock
    private SpendCategoryRepository spendCategoryRepository;

    @Mock
    private BudgetAllocationRepository budgetAllocationRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldReturnTransactions_whenFindAllByEntity() {
        // arrange
        Long entityId = 1L;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .active(true)
                .build();

        Transaction transaction1 = Transaction.builder()
                .id(1L)
                .type(TransactionTypeEnum.DEBIT)
                .build();

        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .type(TransactionTypeEnum.CREDIT)
                .build();

        TransactionResponseDTO dto1 = TransactionResponseDTO.builder()
                .id(1L)
                .type(TransactionTypeEnum.DEBIT)
                .build();

        TransactionResponseDTO dto2 = TransactionResponseDTO.builder()
                .id(2L)
                .type(TransactionTypeEnum.CREDIT)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(transactionRepository.findByOwnershipEntityIdOrderByTransactionDateDesc(entityId))
                .thenReturn(Arrays.asList(transaction1, transaction2));
        when(transactionMapper.toResponseDTO(transaction1)).thenReturn(dto1);
        when(transactionMapper.toResponseDTO(transaction2)).thenReturn(dto2);

        // act
        List<TransactionResponseDTO> result = transactionService.findAllByEntity(entityId);

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(TransactionTypeEnum.DEBIT);
        assertThat(result.get(1).getType()).isEqualTo(TransactionTypeEnum.CREDIT);
    }

    @Test
    void shouldReturnTransactions_whenFindAllByAccount() {
        // arrange
        Long accountId = 1L;

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .active(true)
                .build();

        Transaction transaction = Transaction.builder()
                .id(1L)
                .build();

        TransactionResponseDTO dto = TransactionResponseDTO.builder()
                .id(1L)
                .build();

        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByFinancialAccountIdOrderByTransactionDateDesc(accountId))
                .thenReturn(List.of(transaction));
        when(transactionMapper.toResponseDTO(transaction)).thenReturn(dto);

        // act
        List<TransactionResponseDTO> result = transactionService.findAllByAccount(accountId);

        // assert
        assertThat(result).hasSize(1);
        verify(financialAccountRepository).findByIdAndActiveTrue(accountId);
    }

    @Test
    void shouldReturnTransactions_whenFindAllByAllocation() {
        // arrange
        Long allocationId = 1L;

        BudgetAllocation allocation = BudgetAllocation.builder()
                .id(allocationId)
                .active(true)
                .build();

        Transaction transaction = Transaction.builder()
                .id(1L)
                .build();

        TransactionResponseDTO dto = TransactionResponseDTO.builder()
                .id(1L)
                .build();

        when(budgetAllocationRepository.findByIdAndActiveTrue(allocationId)).thenReturn(Optional.of(allocation));
        when(transactionRepository.findByBudgetAllocationIdOrderByTransactionDateDesc(allocationId))
                .thenReturn(List.of(transaction));
        when(transactionMapper.toResponseDTO(transaction)).thenReturn(dto);

        // act
        List<TransactionResponseDTO> result = transactionService.findAllByAllocation(allocationId);

        // assert
        assertThat(result).hasSize(1);
        verify(budgetAllocationRepository).findByIdAndActiveTrue(allocationId);
    }

    @Test
    void shouldReturnTransaction_whenValidId() {
        // arrange
        Long transactionId = 1L;

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .build();

        TransactionResponseDTO dto = TransactionResponseDTO.builder()
                .id(transactionId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponseDTO(transaction)).thenReturn(dto);

        // act
        TransactionResponseDTO result = transactionService.findById(transactionId);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(transactionId);
    }

    @Test
    void shouldThrowException_whenTransactionNotFound() {
        // arrange
        Long transactionId = 99L;
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> transactionService.findById(transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void shouldCreateDebitTransaction_whenValidRequest() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();
        SpendCategory category = SpendCategory.builder().id(1L).active(true).build();

        Transaction transaction = Transaction.builder().type(TransactionTypeEnum.DEBIT).build();
        Transaction savedTransaction = Transaction.builder().id(1L).type(TransactionTypeEnum.DEBIT).build();
        TransactionResponseDTO response = TransactionResponseDTO.builder().id(1L).type(TransactionTypeEnum.DEBIT).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponseDTO(savedTransaction)).thenReturn(response);

        // act
        TransactionResponseDTO result = transactionService.create(request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionTypeEnum.DEBIT);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void shouldCreateCreditTransaction_whenValidRequest() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .type(TransactionTypeEnum.CREDIT)
                .amount(new BigDecimal("500.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();
        SpendCategory category = SpendCategory.builder().id(1L).active(true).build();

        Transaction transaction = Transaction.builder().type(TransactionTypeEnum.CREDIT).build();
        Transaction savedTransaction = Transaction.builder().id(1L).type(TransactionTypeEnum.CREDIT).build();
        TransactionResponseDTO response = TransactionResponseDTO.builder().id(1L).type(TransactionTypeEnum.CREDIT).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponseDTO(savedTransaction)).thenReturn(response);

        // act
        TransactionResponseDTO result = transactionService.create(request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionTypeEnum.CREDIT);
    }

    @Test
    void shouldCreateTransferTransaction_whenValidRequest() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .type(TransactionTypeEnum.TRANSFER)
                .amount(new BigDecimal("250.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();
        SpendCategory category = SpendCategory.builder().id(1L).active(true).build();

        Transaction transaction = Transaction.builder().type(TransactionTypeEnum.TRANSFER).build();
        Transaction savedTransaction = Transaction.builder().id(1L).type(TransactionTypeEnum.TRANSFER).build();
        TransactionResponseDTO response = TransactionResponseDTO.builder().id(1L).type(TransactionTypeEnum.TRANSFER).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponseDTO(savedTransaction)).thenReturn(response);

        // act
        TransactionResponseDTO result = transactionService.create(request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionTypeEnum.TRANSFER);
    }

    @Test
    void shouldCreateAdjustmentTransaction_whenValidRequest() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .type(TransactionTypeEnum.ADJUSTMENT)
                .amount(new BigDecimal("10.00"))
                .transactionDate(LocalDate.now())
                .description("Correction for duplicate entry")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();
        SpendCategory category = SpendCategory.builder().id(1L).active(true).build();

        Transaction transaction = Transaction.builder().type(TransactionTypeEnum.ADJUSTMENT).build();
        Transaction savedTransaction = Transaction.builder().id(1L).type(TransactionTypeEnum.ADJUSTMENT).build();
        TransactionResponseDTO response = TransactionResponseDTO.builder().id(1L).type(TransactionTypeEnum.ADJUSTMENT).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponseDTO(savedTransaction)).thenReturn(response);

        // act
        TransactionResponseDTO result = transactionService.create(request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionTypeEnum.ADJUSTMENT);
    }

    @Test
    void shouldCreateTransactionWithAllocation_whenAllocationProvided() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .budgetAllocationId(1L) // Optional allocation provided
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("50.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();
        SpendCategory category = SpendCategory.builder().id(1L).active(true).build();
        BudgetAllocation allocation = BudgetAllocation.builder().id(1L).active(true).build();

        Transaction transaction = Transaction.builder().build();
        Transaction savedTransaction = Transaction.builder().id(1L).build();
        TransactionResponseDTO response = TransactionResponseDTO.builder().id(1L).budgetAllocationId(1L).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(budgetAllocationRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(allocation));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponseDTO(savedTransaction)).thenReturn(response);

        // act
        TransactionResponseDTO result = transactionService.create(request);

        // assert
        assertThat(result.getBudgetAllocationId()).isEqualTo(1L);
        verify(budgetAllocationRepository).findByIdAndActiveTrue(1L);
    }

    @Test
    void shouldCreateTransactionWithoutAllocation_whenAllocationNotProvided() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .budgetAllocationId(null) // No allocation
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("50.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();
        SpendCategory category = SpendCategory.builder().id(1L).active(true).build();

        Transaction transaction = Transaction.builder().build();
        Transaction savedTransaction = Transaction.builder().id(1L).build();
        TransactionResponseDTO response = TransactionResponseDTO.builder().id(1L).budgetAllocationId(null).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponseDTO(savedTransaction)).thenReturn(response);

        // act
        TransactionResponseDTO result = transactionService.create(request);

        // assert
        assertThat(result.getBudgetAllocationId()).isNull();
        verify(budgetAllocationRepository, never()).findByIdAndActiveTrue(any());
    }

    @Test
    void shouldThrowException_whenCarryoverTypeSubmitted() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .type(TransactionTypeEnum.CARRYOVER) // Not allowed via API
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        // act & assert
        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("system-generated only");
    }

    @Test
    void shouldThrowException_whenAccountNotFound() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(99L) // Not found
                .spendCategoryId(1L)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Financial account not found");
    }

    @Test
    void shouldThrowException_whenCategoryNotFound() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(99L) // Not found
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Spend category not found");
    }

    @Test
    void shouldThrowException_whenAllocationNotFoundIfProvided() {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(1L)
                .financialAccountId(1L)
                .spendCategoryId(1L)
                .budgetAllocationId(99L) // Not found
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        OwnershipEntity entity = OwnershipEntity.builder().id(1L).active(true).build();
        FinancialAccount account = FinancialAccount.builder().id(1L).active(true).build();
        SpendCategory category = SpendCategory.builder().id(1L).active(true).build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(account));
        when(spendCategoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(budgetAllocationRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Budget allocation not found");
    }
}

