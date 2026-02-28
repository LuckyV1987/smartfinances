package com.smartfinances.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinances.dto.request.UserUpdateRequestDTO;
import com.smartfinances.entity.User;
import com.smartfinances.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturn200_whenGetUserByValidId() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        User savedUser = userRepository.save(user);

        // act & assert
        mockMvc.perform(get("/api/users/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("1234567890"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturn404_whenGetUserByInvalidId() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }

    @Test
    void shouldReturn200_whenUpdateUserWithValidRequest() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();
        User savedUser = userRepository.save(user);

        UserUpdateRequestDTO updateDTO = UserUpdateRequestDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("9876543210")
                .build();

        // act & assert
        mockMvc.perform(put("/api/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.phoneNumber").value("9876543210"));
    }


    @Test
    void shouldReturn404_whenUpdateUserWithInvalidId() throws Exception {
        // arrange
        UserUpdateRequestDTO updateDTO = UserUpdateRequestDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        // act & assert
        mockMvc.perform(put("/api/users/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }

    @Test
    void shouldOnlyUpdateProvidedFields_whenPartialUpdateRequest() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        User savedUser = userRepository.save(user);

        UserUpdateRequestDTO updateDTO = UserUpdateRequestDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        // act & assert
        mockMvc.perform(put("/api/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.phoneNumber").value("1234567890"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-01-01"));
    }

    @Test
    void shouldReturn204_whenDeactivateValidUser() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();
        User savedUser = userRepository.save(user);

        // act & assert
        mockMvc.perform(delete("/api/users/{id}", savedUser.getId()))
                .andExpect(status().isNoContent());

        // verify user has deletedAt set
        User deactivatedUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertThat(deactivatedUser).isNotNull();
        assertThat(deactivatedUser.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldReturn404_whenDeactivateInvalidUser() throws Exception {
        // act & assert
        mockMvc.perform(delete("/api/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }

    @Test
    void shouldReturn404_whenGetDeactivatedUser() throws Exception {
        // arrange - create active user
        User user = User.builder()
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();
        User savedUser = userRepository.save(user);
        Long userId = savedUser.getId();

        // deactivate the user
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        // flush and clear to ensure SQLRestriction is applied
        entityManager.flush();
        entityManager.clear();

        // act & assert - soft deleted users are filtered by @SQLRestriction
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

}


