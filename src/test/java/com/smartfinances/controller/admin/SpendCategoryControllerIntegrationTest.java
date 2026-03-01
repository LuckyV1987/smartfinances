package com.smartfinances.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinances.dto.request.SpendCategoryRequestDTO;
import com.smartfinances.entity.SpendCategory;
import com.smartfinances.entity.enums.SpendCategoryType;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class SpendCategoryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpendCategoryRepository spendCategoryRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        spendCategoryRepository.deleteAll();
    }

    @Test
    void shouldReturn200_whenGetAllCategories() throws Exception {
        // arrange
        SpendCategory category1 = SpendCategory.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .description("Regular employment income")
                .build();

        SpendCategory category2 = SpendCategory.builder()
                .name("Groceries")
                .type(SpendCategoryType.EXPENSE)
                .description("Food shopping")
                .build();

        spendCategoryRepository.save(category1);
        spendCategoryRepository.save(category2);

        // act & assert
        mockMvc.perform(get("/api/admin/spend-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void shouldReturn200_whenGetCategoriesByType() throws Exception {
        // arrange
        SpendCategory inflow = SpendCategory.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .build();

        SpendCategory expense = SpendCategory.builder()
                .name("Groceries")
                .type(SpendCategoryType.EXPENSE)
                .build();

        spendCategoryRepository.save(inflow);
        spendCategoryRepository.save(expense);

        // act & assert
        mockMvc.perform(get("/api/admin/spend-categories")
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Groceries"))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    @Test
    void shouldReturn200_whenGetCategoryById() throws Exception {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .description("Regular employment income")
                .build();

        SpendCategory savedCategory = spendCategoryRepository.save(category);

        // act & assert
        mockMvc.perform(get("/api/admin/spend-categories/{id}", savedCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedCategory.getId()))
                .andExpect(jsonPath("$.name").value("Salary"))
                .andExpect(jsonPath("$.type").value("INFLOW"))
                .andExpect(jsonPath("$.description").value("Regular employment income"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn404_whenGetInvalidCategory() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/admin/spend-categories/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Spend category not found")));
    }

    @Test
    void shouldReturn201_whenCreateValidCategory() throws Exception {
        // arrange
        SpendCategoryRequestDTO requestDTO = SpendCategoryRequestDTO.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .description("Regular employment income")
                .build();

        // act & assert
        mockMvc.perform(post("/api/admin/spend-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Salary"))
                .andExpect(jsonPath("$.type").value("INFLOW"))
                .andExpect(jsonPath("$.description").value("Regular employment income"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn400_whenCreateDuplicateName() throws Exception {
        // arrange
        SpendCategory existingCategory = SpendCategory.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .build();

        spendCategoryRepository.save(existingCategory);

        SpendCategoryRequestDTO requestDTO = SpendCategoryRequestDTO.builder()
                .name("salary") // case insensitive
                .type(SpendCategoryType.INFLOW)
                .build();

        // act & assert
        mockMvc.perform(post("/api/admin/spend-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void shouldReturn200_whenUpdateCategory() throws Exception {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .description("Old description")
                .build();

        SpendCategory savedCategory = spendCategoryRepository.save(category);

        SpendCategoryRequestDTO updateDTO = SpendCategoryRequestDTO.builder()
                .name("Updated Salary")
                .type(SpendCategoryType.INFLOW)
                .description("New description")
                .build();

        // act & assert
        mockMvc.perform(put("/api/admin/spend-categories/{id}", savedCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Salary"))
                .andExpect(jsonPath("$.description").value("New description"));
    }

    @Test
    void shouldReturn204_whenDeactivateCategory() throws Exception {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .build();

        SpendCategory savedCategory = spendCategoryRepository.save(category);

        // act & assert
        mockMvc.perform(delete("/api/admin/spend-categories/{id}", savedCategory.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenGetDeactivatedCategory() throws Exception {
        // arrange
        SpendCategory category = SpendCategory.builder()
                .name("Salary")
                .type(SpendCategoryType.INFLOW)
                .build();

        SpendCategory savedCategory = spendCategoryRepository.save(category);

        // Deactivate
        mockMvc.perform(delete("/api/admin/spend-categories/{id}", savedCategory.getId()))
                .andExpect(status().isNoContent());

        // act & assert - try to get deactivated category
        mockMvc.perform(get("/api/admin/spend-categories/{id}", savedCategory.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Spend category not found")));
    }
}

