package com.smartfinances.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinances.dto.request.BudgetAllocationRequestDTO;
import com.smartfinances.dto.request.BudgetAllocationUpdateRequestDTO;
import com.smartfinances.dto.request.CategoryLinkRequestDTO;
import com.smartfinances.entity.BudgetAllocation;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.SpendCategory;
import com.smartfinances.entity.enums.AllocationIntervalEnum;
import com.smartfinances.entity.enums.AllocationTypeEnum;
import com.smartfinances.entity.enums.AllocationUnitEnum;
import com.smartfinances.entity.enums.OwnershipEntityType;
import com.smartfinances.entity.enums.SpendCategoryType;
import com.smartfinances.repository.BudgetAllocationCategoryRepository;
import com.smartfinances.repository.BudgetAllocationRepository;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.SpendCategoryRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class BudgetAllocationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BudgetAllocationRepository budgetAllocationRepository;

    @Autowired
    private BudgetAllocationCategoryRepository budgetAllocationCategoryRepository;

    @Autowired
    private OwnershipEntityRepository ownershipEntityRepository;

    @Autowired
    private SpendCategoryRepository spendCategoryRepository;

    private OwnershipEntity testEntity;
    private SpendCategory testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up
        budgetAllocationCategoryRepository.deleteAll();
        budgetAllocationRepository.deleteAll();
        spendCategoryRepository.deleteAll();
        ownershipEntityRepository.deleteAll();

        // Create test ownership entity
        testEntity = OwnershipEntity.builder()
                .name("Test Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();
        testEntity = ownershipEntityRepository.save(testEntity);

        // Create test spend category
        testCategory = SpendCategory.builder()
                .name("Groceries")
                .type(SpendCategoryType.EXPENSE)
                .active(true)
                .build();
        testCategory = spendCategoryRepository.save(testCategory);
    }

    @Test
    void shouldReturn200_whenGetAllocationsByEntity() throws Exception {
        // arrange
        BudgetAllocation allocation1 = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries Budget")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();

        BudgetAllocation allocation2 = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Emergency Fund")
                .type(AllocationTypeEnum.SAVINGS)
                .active(true)
                .build();

        budgetAllocationRepository.save(allocation1);
        budgetAllocationRepository.save(allocation2);

        // act & assert
        mockMvc.perform(get("/api/budget-allocations")
                        .param("ownershipEntityId", testEntity.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].currentBalance", is(0)));
    }

    @Test
    void shouldReturn200_whenGetAllocationsByType() throws Exception {
        // arrange
        BudgetAllocation budget = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();

        BudgetAllocation savings = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Emergency")
                .type(AllocationTypeEnum.SAVINGS)
                .active(true)
                .build();

        budgetAllocationRepository.save(budget);
        budgetAllocationRepository.save(savings);

        // act & assert
        mockMvc.perform(get("/api/budget-allocations")
                        .param("ownershipEntityId", testEntity.getId().toString())
                        .param("type", "BUDGET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("BUDGET")));
    }

    @Test
    void shouldReturn200_whenGetAllocationById() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        // act & assert
        mockMvc.perform(get("/api/budget-allocations/{id}", allocation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(allocation.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Groceries")))
                .andExpect(jsonPath("$.type", is("BUDGET")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturn200_whenCheckBalance() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .targetAmount(new BigDecimal("500.00"))
                .targetUnit(AllocationUnitEnum.FIXED)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        // act & assert
        mockMvc.perform(get("/api/budget-allocations/{id}/balance", allocation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance", is(0)))
                .andExpect(jsonPath("$.remainingAmount", is(500.0)));
    }

    @Test
    void shouldReturn404_whenGetInvalidAllocation() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/budget-allocations/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void shouldReturn404_whenGetInactiveAllocation() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Inactive")
                .type(AllocationTypeEnum.BUDGET)
                .active(false)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        // act & assert
        mockMvc.perform(get("/api/budget-allocations/{id}", allocation.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn201_whenCreateFixedAllocation() throws Exception {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Groceries Budget")
                .description("Monthly groceries")
                .type(AllocationTypeEnum.BUDGET)
                .targetAmount(new BigDecimal("500.00"))
                .targetUnit(AllocationUnitEnum.FIXED)
                .interval(AllocationIntervalEnum.MONTHLY)
                .rollover(false)
                .build();

        // act & assert
        mockMvc.perform(post("/api/budget-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Groceries Budget")))
                .andExpect(jsonPath("$.type", is("BUDGET")))
                .andExpect(jsonPath("$.targetAmount", is(500.0)))
                .andExpect(jsonPath("$.targetUnit", is("FIXED")))
                .andExpect(jsonPath("$.currentBalance", is(0)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturn201_whenCreatePercentageAllocation() throws Exception {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Savings")
                .type(AllocationTypeEnum.SAVINGS)
                .targetAmount(new BigDecimal("0.20"))
                .targetUnit(AllocationUnitEnum.PERCENTAGE)
                .interval(AllocationIntervalEnum.MONTHLY)
                .build();

        // act & assert
        mockMvc.perform(post("/api/budget-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetAmount", is(0.2)))
                .andExpect(jsonPath("$.targetUnit", is("PERCENTAGE")));
    }

    @Test
    void shouldReturn201_whenCreateOneTimeAllocation() throws Exception {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Tax Refund")
                .type(AllocationTypeEnum.INCOME)
                .interval(null) // one-time
                .build();

        // act & assert
        mockMvc.perform(post("/api/budget-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Tax Refund")))
                .andExpect(jsonPath("$.interval").isEmpty());
    }

    @Test
    void shouldReturn400_whenSinkingFundHasNoTargetDate() throws Exception {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Vacation Fund")
                .type(AllocationTypeEnum.SINKING_FUND)
                .targetDate(null) // Missing required field
                .build();

        // act & assert
        mockMvc.perform(post("/api/budget-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("target date")));
    }

    @Test
    void shouldReturn400_whenRolloverWithNoInterval() throws Exception {
        // arrange
        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .rollover(true)
                .interval(null) // Missing required field
                .build();

        // act & assert
        mockMvc.perform(post("/api/budget-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("interval")));
    }

    @Test
    void shouldReturn400_whenDuplicateNameForEntity() throws Exception {
        // arrange - create existing allocation
        BudgetAllocation existing = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        budgetAllocationRepository.save(existing);

        BudgetAllocationRequestDTO request = BudgetAllocationRequestDTO.builder()
                .ownershipEntityId(testEntity.getId())
                .name("Groceries") // Duplicate name
                .type(AllocationTypeEnum.BUDGET)
                .build();

        // act & assert
        mockMvc.perform(post("/api/budget-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void shouldReturn200_whenUpdateAllocation() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Old Name")
                .description("Old description")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        BudgetAllocationUpdateRequestDTO request = BudgetAllocationUpdateRequestDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        // act & assert
        mockMvc.perform(put("/api/budget-allocations/{id}", allocation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.type", is("BUDGET"))); // Type should not change
    }

    @Test
    void shouldReturn204_whenDeactivateAllocation() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Test")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        // act & assert
        mockMvc.perform(delete("/api/budget-allocations/{id}", allocation.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenGetDeactivatedAllocation() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Test")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        // Deactivate
        mockMvc.perform(delete("/api/budget-allocations/{id}", allocation.getId()))
                .andExpect(status().isNoContent());

        // act & assert
        mockMvc.perform(get("/api/budget-allocations/{id}", allocation.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200_whenLinkCategory() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries Budget")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        CategoryLinkRequestDTO request = CategoryLinkRequestDTO.builder()
                .spendCategoryId(testCategory.getId())
                .build();

        // act & assert
        mockMvc.perform(post("/api/budget-allocations/{id}/categories", allocation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testCategory.getId().intValue())))
                .andExpect(jsonPath("$[0].name", is("Groceries")));
    }

    @Test
    void shouldReturn400_whenLinkDuplicateCategory() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries Budget")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        CategoryLinkRequestDTO request = CategoryLinkRequestDTO.builder()
                .spendCategoryId(testCategory.getId())
                .build();

        // Link once
        mockMvc.perform(post("/api/budget-allocations/{id}/categories", allocation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Try to link again
        mockMvc.perform(post("/api/budget-allocations/{id}/categories", allocation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already linked")));
    }

    @Test
    void shouldReturn200_whenUnlinkCategory() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries Budget")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        // Link category first
        CategoryLinkRequestDTO linkRequest = CategoryLinkRequestDTO.builder()
                .spendCategoryId(testCategory.getId())
                .build();

        mockMvc.perform(post("/api/budget-allocations/{id}/categories", allocation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk());

        // act & assert - unlink
        mockMvc.perform(delete("/api/budget-allocations/{id}/categories/{categoryId}",
                        allocation.getId(), testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn404_whenUnlinkCategoryNotFound() throws Exception {
        // arrange
        BudgetAllocation allocation = BudgetAllocation.builder()
                .ownershipEntity(testEntity)
                .name("Groceries Budget")
                .type(AllocationTypeEnum.BUDGET)
                .active(true)
                .build();
        allocation = budgetAllocationRepository.save(allocation);

        // act & assert
        mockMvc.perform(delete("/api/budget-allocations/{id}/categories/{categoryId}",
                        allocation.getId(), 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }
}


