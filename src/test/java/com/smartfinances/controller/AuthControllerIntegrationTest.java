package com.smartfinances.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinances.dto.request.UserRequestDTO;
import com.smartfinances.entity.User;
import com.smartfinances.repository.UserRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturn201_whenValidRegistration() throws Exception {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("newuser@example.com")
                .password("Password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("1234567890"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturn400_whenEmailAlreadyExists() throws Exception {
        // arrange
        User existingUser = User.builder()
                .email("existing@example.com")
                .password("hashedPassword")
                .firstName("Existing")
                .lastName("User")
                .build();
        userRepository.save(existingUser);

        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("existing@example.com")
                .password("Password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email already exists")));
    }

    @Test
    void shouldReturn400_whenPasswordTooShort() throws Exception {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("test@example.com")
                .password("Pass12")
                .firstName("John")
                .lastName("Doe")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void shouldReturn400_whenPasswordMissingNumber() throws Exception {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("test@example.com")
                .password("Password")
                .firstName("John")
                .lastName("Doe")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(containsString("must contain at least one letter and one number")));
    }

    @Test
    void shouldReturn400_whenPasswordMissingLetter() throws Exception {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("test@example.com")
                .password("12345678")
                .firstName("John")
                .lastName("Doe")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value(containsString("must contain at least one letter and one number")));
    }

    @Test
    void shouldReturn400_whenEmailInvalid() throws Exception {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("invalid-email")
                .password("Password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value(containsString("must be valid")));
    }

    @Test
    void shouldReturn400_whenRequiredFieldsMissing() throws Exception {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value(containsString("required")))
                .andExpect(jsonPath("$.errors.password").value(containsString("required")))
                .andExpect(jsonPath("$.errors.firstName").value(containsString("required")))
                .andExpect(jsonPath("$.errors.lastName").value(containsString("required")));
    }

}

