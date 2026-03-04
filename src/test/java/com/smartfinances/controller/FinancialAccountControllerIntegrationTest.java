package com.smartfinances.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinances.dto.request.FinancialAccountRequestDTO;
import com.smartfinances.dto.request.FinancialAccountUpdateRequestDTO;
import com.smartfinances.entity.FinancialAccount;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.enums.AccountTypeEnum;
import com.smartfinances.entity.enums.OwnershipEntityType;
import com.smartfinances.repository.FinancialAccountRepository;
import com.smartfinances.repository.OwnershipEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class FinancialAccountControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinancialAccountRepository financialAccountRepository;

    @Autowired
    private OwnershipEntityRepository ownershipEntityRepository;

    private OwnershipEntity testEntity;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up
        financialAccountRepository.deleteAll();
        ownershipEntityRepository.deleteAll();

        // Create test ownership entity
        testEntity = OwnershipEntity.builder()
                .name("Test Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();
        testEntity = ownershipEntityRepository.save(testEntity);
    }

    @Test
    void shouldReturn200_whenGetAccountsByEntity() throws Exception {
        // arrange
        FinancialAccount account1 = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Main Checking")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();

        FinancialAccount account2 = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Savings")
                .type(AccountTypeEnum.SAVINGS)
                .currencyCode("USD")
                .active(true)
                .build();

        financialAccountRepository.save(account1);
        financialAccountRepository.save(account2);

        // act & assert
        mockMvc.perform(get("/api/financial-accounts")
                        .param("ownershipEntityId", testEntity.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].currentBalance", is(0)));
    }

    @Test
    void shouldReturn200_whenGetAccountsByType() throws Exception {
        // arrange
        FinancialAccount checking = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Checking")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();

        FinancialAccount savings = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Savings")
                .type(AccountTypeEnum.SAVINGS)
                .currencyCode("USD")
                .active(true)
                .build();

        financialAccountRepository.save(checking);
        financialAccountRepository.save(savings);

        // act & assert
        mockMvc.perform(get("/api/financial-accounts")
                        .param("ownershipEntityId", testEntity.getId().toString())
                        .param("type", "CHECKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("CHECKING")));
    }

    @Test
    void shouldReturn200_whenGetAccountById() throws Exception {
        // arrange
        FinancialAccount account = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Main Checking")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();
        account = financialAccountRepository.save(account);

        // act & assert
        mockMvc.perform(get("/api/financial-accounts/{id}", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(account.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Main Checking")))
                .andExpect(jsonPath("$.type", is("CHECKING")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturn200_whenGetBalance() throws Exception {
        // arrange
        FinancialAccount account = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Main Checking")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();
        account = financialAccountRepository.save(account);

        // act & assert
        mockMvc.perform(get("/api/financial-accounts/{id}/balance", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance", is(0)));
    }

    @Test
    void shouldReturn404_whenGetInvalidAccount() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/financial-accounts/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void shouldReturn404_whenGetInactiveAccount() throws Exception {
        // arrange
        FinancialAccount account = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Inactive Account")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(false)
                .build();
        account = financialAccountRepository.save(account);

        // act & assert
        mockMvc.perform(get("/api/financial-accounts/{id}", account.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn201_whenCreateAccount() throws Exception {
        // arrange
        FinancialAccountRequestDTO request = FinancialAccountRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Main Checking")
                .description("Primary checking account")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .build();

        // act & assert
        mockMvc.perform(post("/api/financial-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Main Checking")))
                .andExpect(jsonPath("$.type", is("CHECKING")))
                .andExpect(jsonPath("$.currencyCode", is("USD")))
                .andExpect(jsonPath("$.currentBalance", is(0)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturn201_whenCreateAccountWithDefaultCurrency() throws Exception {
        // arrange
        FinancialAccountRequestDTO request = FinancialAccountRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Cash Wallet")
                .type(AccountTypeEnum.CASH)
                .currencyCode(null) // Should default to USD
                .build();

        // act & assert
        mockMvc.perform(post("/api/financial-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currencyCode", is("USD")));
    }

    @Test
    void shouldReturn400_whenDuplicateNameForEntity() throws Exception {
        // arrange - create existing account
        FinancialAccount existing = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Checking")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();
        financialAccountRepository.save(existing);

        FinancialAccountRequestDTO request = FinancialAccountRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Checking") // Duplicate name
                .type(AccountTypeEnum.CHECKING)
                .build();

        // act & assert
        mockMvc.perform(post("/api/financial-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void shouldReturn200_whenUpdateAccount() throws Exception {
        // arrange
        FinancialAccount account = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Old Name")
                .description("Old description")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();
        account = financialAccountRepository.save(account);

        FinancialAccountUpdateRequestDTO request = FinancialAccountUpdateRequestDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        // act & assert
        mockMvc.perform(put("/api/financial-accounts/{id}", account.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.type", is("CHECKING"))) // Type should not change
                .andExpect(jsonPath("$.currencyCode", is("USD"))); // Currency should not change
    }

    @Test
    void shouldReturn204_whenDeactivateAccount() throws Exception {
        // arrange
        FinancialAccount account = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Test Account")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();
        account = financialAccountRepository.save(account);

        // act & assert
        mockMvc.perform(delete("/api/financial-accounts/{id}", account.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenGetDeactivatedAccount() throws Exception {
        // arrange
        FinancialAccount account = FinancialAccount.builder()
                .ownershipEntity(testEntity)
                .name("Test Account")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .active(true)
                .build();
        account = financialAccountRepository.save(account);

        // Deactivate
        mockMvc.perform(delete("/api/financial-accounts/{id}", account.getId()))
                .andExpect(status().isNoContent());

        // act & assert
        mockMvc.perform(get("/api/financial-accounts/{id}", account.getId()))
                .andExpect(status().isNotFound());
    }
}

