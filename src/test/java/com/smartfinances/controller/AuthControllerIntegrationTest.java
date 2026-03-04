package com.smartfinances.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinances.dto.request.LoginRequestDTO;
import com.smartfinances.dto.request.LogoutRequestDTO;
import com.smartfinances.dto.request.RefreshRequestDTO;
import com.smartfinances.dto.request.UserRequestDTO;
import com.smartfinances.entity.RefreshToken;
import com.smartfinances.entity.User;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.OwnershipMembershipRepository;
import com.smartfinances.repository.RefreshTokenRepository;
import com.smartfinances.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OwnershipEntityRepository ownershipEntityRepository;

    @Autowired
    private OwnershipMembershipRepository ownershipMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        refreshTokenRepository.deleteAll();
        ownershipMembershipRepository.deleteAll();
        ownershipEntityRepository.deleteAll();
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

    // ===== LOGIN ENDPOINT TESTS =====

    @Test
    void shouldReturn200_whenLoginWithValidCredentials() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123"))
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void shouldReturn401_whenLoginWithWrongPassword() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123"))
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .email("test@example.com")
                .password("WrongPassword123")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldReturn401_whenLoginWithUnknownEmail() throws Exception {
        // arrange
        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .email("unknown@example.com")
                .password("Password123")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldReturn403_whenLoginWithDeactivatedAccount() throws Exception {
        // arrange
        User user = User.builder()
                .email("deactivated@example.com")
                .password(passwordEncoder.encode("Password123"))
                .firstName("John")
                .lastName("Doe")
                .deletedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .email("deactivated@example.com")
                .password("Password123")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is deactivated"));
    }

    // ===== REFRESH ENDPOINT TESTS =====

    @Test
    void shouldReturn200_whenRefreshWithValidToken() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123"))
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-refresh-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);

        RefreshRequestDTO refreshDTO = RefreshRequestDTO.builder()
                .refreshToken("valid-refresh-token")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void shouldReturn401_whenRefreshWithRevokedToken() throws Exception {
        // arrange
        RefreshRequestDTO refreshDTO = RefreshRequestDTO.builder()
                .refreshToken("revoked-or-nonexistent-token")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
    }

    @Test
    void shouldReturn401_whenRefreshWithExpiredToken() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123"))
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-refresh-token")
                .user(user)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        refreshTokenRepository.save(expiredToken);

        RefreshRequestDTO refreshDTO = RefreshRequestDTO.builder()
                .refreshToken("expired-refresh-token")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
    }

    // ===== LOGOUT ENDPOINT TESTS =====

    @Test
    void shouldReturn204_whenLogoutWithValidToken() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123"))
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        // Login to get tokens
        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(responseBody).get("refreshToken").asText();

        LogoutRequestDTO logoutDTO = LogoutRequestDTO.builder()
                .refreshToken(refreshToken)
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutDTO)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn401_whenLogoutWithoutAuthHeader() throws Exception {
        // arrange
        LogoutRequestDTO logoutDTO = LogoutRequestDTO.builder()
                .refreshToken("some-token")
                .build();

        // act & assert
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn204_whenLogoutAll() throws Exception {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123"))
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        // Login to get tokens
        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .email("test@example.com")
                .password("Password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();

        // act & assert
        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

}

