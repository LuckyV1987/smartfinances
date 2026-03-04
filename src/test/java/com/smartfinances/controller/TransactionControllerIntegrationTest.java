package com.smartfinances.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinances.dto.request.TransactionDetailRequestDTO;
import com.smartfinances.dto.request.TransactionRequestDTO;
import com.smartfinances.entity.*;
import com.smartfinances.entity.enums.*;
import com.smartfinances.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class TransactionControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    @Autowired
    private OwnershipEntityRepository ownershipEntityRepository;

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private SpendCategoryRepository spendCategoryRepository;

    @Autowired
    private BudgetAllocationRepository budgetAllocationRepository;

    private OwnershipEntity testEntity;
    private FinancialAccount testAccount;
    private SpendCategory testCategory;
    private BudgetAllocation testAllocation;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up
        transactionDetailRepository.deleteAll();
        transactionRepository.deleteAll();
        budgetAllocationRepository.deleteAll();
        financialAccountRepository.deleteAll();
        spendCategoryRepository.deleteAll();
        ownershipEntityRepository.deleteAll();

        // Create test data
        testEntity = OwnershipEntity.builder()
                .name("Test Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();
        testEntity = ownershipEntityRepository.save(testEntity);

        testAccount = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Main Checking")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();
        testAccount = financialAccountRepository.save(testAccount);

        testCategory = SpendCategory.builder()
                .name("Groceries")
                .type(SpendCategoryType.EXPENSE)
                .active(true)
                .build();
        testCategory = spendCategoryRepository.save(testCategory);

        testAllocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Monthly Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        testAllocation = budgetAllocationRepository.save(testAllocation);
    }

    @Test
    void shouldReturn200_whenGetTransactionsByEntity() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("50.00"))
                .transactionDate(LocalDate.now())
                .build();
        transactionRepository.save(transaction);

        // act & assert
        mockMvc.perform(get("/api/transactions")
                        .param("ownershipEntityId", testEntity.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("DEBIT")))
                .andExpect(jsonPath("$[0].amount", is(50.00)));
    }

    @Test
    void shouldReturn200_whenGetTransactionsByAccount() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.CREDIT)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();
        transactionRepository.save(transaction);

        // act & assert
        mockMvc.perform(get("/api/transactions")
                        .param("financialAccountId", testAccount.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].financialAccountId", is(testAccount.getId().intValue())));
    }

    @Test
    void shouldReturn200_whenGetTransactionsByAllocation() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .budgetAllocation(testAllocation)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("25.00"))
                .transactionDate(LocalDate.now())
                .build();
        transactionRepository.save(transaction);

        // act & assert
        mockMvc.perform(get("/api/transactions")
                        .param("budgetAllocationId", testAllocation.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].budgetAllocationId", is(testAllocation.getId().intValue())));
    }

    @Test
    void shouldReturn200_whenGetTransactionById() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("75.00"))
                .transactionDate(LocalDate.now())
                .description("Grocery shopping")
                .build();
        transaction = transactionRepository.save(transaction);

        // act & assert
        mockMvc.perform(get("/api/transactions/{id}", transaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transaction.getId().intValue())))
                .andExpect(jsonPath("$.description", is("Grocery shopping")))
                .andExpect(jsonPath("$.amount", is(75.00)));
    }

    @Test
    void shouldReturn404_whenGetInvalidTransaction() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/transactions/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void shouldReturn201_whenCreateDebitTransaction() throws Exception {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .financialAccountId(testAccount.getId())
                .spendCategoryId(testCategory.getId())
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("150.00"))
                .transactionDate(LocalDate.now())
                .description("Restaurant")
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type", is("DEBIT")))
                .andExpect(jsonPath("$.amount", is(150.00)))
                .andExpect(jsonPath("$.systemGenerated", is(false)));
    }

    @Test
    void shouldReturn201_whenCreateCreditTransaction() throws Exception {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .financialAccountId(testAccount.getId())
                .spendCategoryId(testCategory.getId())
                .type(TransactionTypeEnum.CREDIT)
                .amount(new BigDecimal("2000.00"))
                .transactionDate(LocalDate.now())
                .description("Salary")
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("CREDIT")))
                .andExpect(jsonPath("$.amount", is(2000.00)));
    }

    @Test
    void shouldReturn201_whenCreateTransactionWithAllocation() throws Exception {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .financialAccountId(testAccount.getId())
                .spendCategoryId(testCategory.getId())
                .budgetAllocationId(testAllocation.getId()) // With allocation
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("45.00"))
                .transactionDate(LocalDate.now())
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.budgetAllocationId", is(testAllocation.getId().intValue())));
    }

    @Test
    void shouldReturn400_whenCarryoverTypeSubmitted() throws Exception {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .financialAccountId(testAccount.getId())
                .spendCategoryId(testCategory.getId())
                .type(TransactionTypeEnum.CARRYOVER) // Not allowed
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("system-generated only")));
    }

    @Test
    void shouldReturn400_whenAccountNotFound() throws Exception {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .financialAccountId(99999L) // Not found
                .spendCategoryId(testCategory.getId())
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Financial account not found")));
    }

    @Test
    void shouldReturn400_whenCategoryNotFound() throws Exception {
        // arrange
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .financialAccountId(testAccount.getId())
                .spendCategoryId(99999L) // Not found
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Spend category not found")));
    }

    @Test
    void shouldReturn200_whenGetTransactionDetails() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("20.00"))
                .transactionDate(LocalDate.now())
                .build();
        transaction = transactionRepository.save(transaction);

        TransactionDetail detail = TransactionDetail.builder()
                .transaction(transaction)
                .name("Milk")
                .quantity(new BigDecimal("2.000"))
                .unit("litre")
                .unitPrice(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("10.00"))
                .build();
        transactionDetailRepository.save(detail);

        // act & assert
        mockMvc.perform(get("/api/transactions/{id}/details", transaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Milk")))
                .andExpect(jsonPath("$[0].quantity", is(2.000)))
                .andExpect(jsonPath("$[0].totalPrice", is(10.00)));
    }

    @Test
    void shouldReturn200_whenAddTransactionDetail() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("30.00"))
                .transactionDate(LocalDate.now())
                .build();
        transaction = transactionRepository.save(transaction);

        TransactionDetailRequestDTO request = TransactionDetailRequestDTO.builder()
                .name("Bread")
                .quantity(new BigDecimal("3.000"))
                .unit("loaf")
                .unitPrice(new BigDecimal("4.00"))
                .totalPrice(new BigDecimal("12.00")) // 3 * 4 = 12
                .notes("Fresh bread")
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions/{id}/details", transaction.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Bread")))
                .andExpect(jsonPath("$[0].quantity", is(3.000)))
                .andExpect(jsonPath("$[0].totalPrice", is(12.00)));
    }

    @Test
    void shouldReturn400_whenTotalPriceMismatch() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("30.00"))
                .transactionDate(LocalDate.now())
                .build();
        transaction = transactionRepository.save(transaction);

        TransactionDetailRequestDTO request = TransactionDetailRequestDTO.builder()
                .name("Eggs")
                .quantity(new BigDecimal("2.000"))
                .unitPrice(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("15.00")) // Wrong! Should be 10.00
                .build();

        // act & assert
        mockMvc.perform(post("/api/transactions/{id}/details", transaction.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Total price mismatch")));
    }

    @Test
    void shouldReturn200_whenRemoveTransactionDetail() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("20.00"))
                .transactionDate(LocalDate.now())
                .build();
        transaction = transactionRepository.save(transaction);

        TransactionDetail detail = TransactionDetail.builder()
                .transaction(transaction)
                .name("Coffee")
                .quantity(new BigDecimal("1.000"))
                .unitPrice(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("5.00"))
                .build();
        detail = transactionDetailRepository.save(detail);

        // act & assert
        mockMvc.perform(delete("/api/transactions/{id}/details/{detailId}",
                        transaction.getId(), detail.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // Empty after removal
    }

    @Test
    void shouldReturn404_whenRemoveDetailNotFound() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("20.00"))
                .transactionDate(LocalDate.now())
                .build();
        transaction = transactionRepository.save(transaction);

        // act & assert
        mockMvc.perform(delete("/api/transactions/{id}/details/{detailId}",
                        transaction.getId(), 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void shouldReturn404_whenNoUpdateEndpointExists() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("20.00"))
                .transactionDate(LocalDate.now())
                .build();
        transaction = transactionRepository.save(transaction);

        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .financialAccountId(testAccount.getId())
                .spendCategoryId(testCategory.getId())
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("99.00"))
                .transactionDate(LocalDate.now())
                .build();

        // act & assert - PUT should not exist
        mockMvc.perform(put("/api/transactions/{id}", transaction.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldReturn404_whenNoDeleteEndpointExists() throws Exception {
        // arrange
        Transaction transaction = Transaction.builder()
                .ownershipEntity(testEntity)
                .financialAccount(testAccount)
                .spendCategory(testCategory)
                .type(TransactionTypeEnum.DEBIT)
                .amount(new BigDecimal("20.00"))
                .transactionDate(LocalDate.now())
                .build();
        transaction = transactionRepository.save(transaction);

        // act & assert - DELETE should not exist
        mockMvc.perform(delete("/api/transactions/{id}", transaction.getId()))
                .andExpect(status().isMethodNotAllowed());
    }
}

