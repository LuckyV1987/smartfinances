package com.smartfinances.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinances.dto.request.MembershipRequestDTO;
import com.smartfinances.dto.request.OwnershipEntityRequestDTO;
import com.smartfinances.dto.request.OwnershipEntityUpdateRequestDTO;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.OwnershipMembership;
import com.smartfinances.entity.User;
import com.smartfinances.entity.enums.OwnershipEntityType;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.OwnershipMembershipRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class OwnershipEntityControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OwnershipEntityRepository ownershipEntityRepository;

    @Autowired
    private OwnershipMembershipRepository ownershipMembershipRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private OwnershipEntity testEntity;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Clean up
        ownershipMembershipRepository.deleteAll();
        ownershipEntityRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();
        testUser = userRepository.save(testUser);

        // Create test entity
        testEntity = OwnershipEntity.builder()
                .name("Test Entity")
                .type(OwnershipEntityType.PERSONAL)
                .description("Test description")
                .active(true)
                .build();
        testEntity = ownershipEntityRepository.save(testEntity);
    }

    @Test
    void shouldReturn200_whenGetAllEntities() throws Exception {
        // arrange
        OwnershipEntity entity2 = OwnershipEntity.builder()
                .name("Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();
        ownershipEntityRepository.save(entity2);

        // act & assert
        mockMvc.perform(get("/api/ownership-entities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test Entity")))
                .andExpect(jsonPath("$[1].name", is("Household")));
    }

    @Test
    void shouldReturn200_whenGetEntitiesByType() throws Exception {
        // arrange
        OwnershipEntity householdEntity = OwnershipEntity.builder()
                .name("Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();
        ownershipEntityRepository.save(householdEntity);

        // act & assert
        mockMvc.perform(get("/api/ownership-entities?type=PERSONAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("PERSONAL")));
    }

    @Test
    void shouldReturn200_whenGetEntityById() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/ownership-entities/{id}", testEntity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testEntity.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Entity")))
                .andExpect(jsonPath("$.type", is("PERSONAL")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void shouldReturn404_whenGetInvalidEntity() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/ownership-entities/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404_whenGetInactiveEntity() throws Exception {
        // arrange
        testEntity.setActive(false);
        ownershipEntityRepository.save(testEntity);

        // act & assert
        mockMvc.perform(get("/api/ownership-entities/{id}", testEntity.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200_whenGetEntitiesByUser() throws Exception {
        // arrange
        OwnershipMembership membership = OwnershipMembership.builder()
                .user(testUser)
                .ownershipEntity(testEntity)
                .active(true)
                .build();
        ownershipMembershipRepository.save(membership);

        // act & assert
        mockMvc.perform(get("/api/ownership-entities/user/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testEntity.getId().intValue())));
    }

    @Test
    void shouldReturn200_whenGetMembersByEntity() throws Exception {
        // arrange
        OwnershipMembership membership = OwnershipMembership.builder()
                .user(testUser)
                .ownershipEntity(testEntity)
                .active(true)
                .build();
        ownershipMembershipRepository.save(membership);

        // act & assert
        mockMvc.perform(get("/api/ownership-entities/{id}/members", testEntity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(testUser.getId().intValue())));
    }

    @Test
    void shouldReturn201_whenCreateEntity() throws Exception {
        // arrange
        OwnershipEntityRequestDTO request = OwnershipEntityRequestDTO.builder()
                .name("New Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("New household description")
                .build();

        // act & assert
        mockMvc.perform(post("/api/ownership-entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Household")))
                .andExpect(jsonPath("$.type", is("HOUSEHOLD")))
                .andExpect(jsonPath("$.description", is("New household description")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void shouldReturn200_whenUpdateEntity() throws Exception {
        // arrange
        OwnershipEntityUpdateRequestDTO request = OwnershipEntityUpdateRequestDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        // act & assert
        mockMvc.perform(put("/api/ownership-entities/{id}", testEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.type", is("PERSONAL"))); // Type should not change
    }

    @Test
    void shouldReturn204_whenDeactivateEntity() throws Exception {
        // act & assert
        mockMvc.perform(delete("/api/ownership-entities/{id}", testEntity.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenGetDeactivatedEntity() throws Exception {
        // arrange
        testEntity.setActive(false);
        ownershipEntityRepository.save(testEntity);

        // act & assert
        mockMvc.perform(get("/api/ownership-entities/{id}", testEntity.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn201_whenAddMember() throws Exception {
        // arrange
        User newUser = User.builder()
                .email("newuser@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .build();
        newUser = userRepository.save(newUser);

        MembershipRequestDTO request = MembershipRequestDTO.builder()
                .userId(newUser.getId())
                .build();

        // act & assert
        mockMvc.perform(post("/api/ownership-entities/{id}/members", testEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(newUser.getId().intValue())))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void shouldReturn400_whenAddDuplicateMember() throws Exception {
        // arrange
        OwnershipMembership existingMembership = OwnershipMembership.builder()
                .user(testUser)
                .ownershipEntity(testEntity)
                .active(true)
                .build();
        ownershipMembershipRepository.save(existingMembership);

        MembershipRequestDTO request = MembershipRequestDTO.builder()
                .userId(testUser.getId())
                .build();

        // act & assert
        mockMvc.perform(post("/api/ownership-entities/{id}/members", testEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn204_whenRemoveMember() throws Exception {
        // arrange
        OwnershipMembership membership = OwnershipMembership.builder()
                .user(testUser)
                .ownershipEntity(testEntity)
                .active(true)
                .build();
        ownershipMembershipRepository.save(membership);

        // act & assert
        mockMvc.perform(delete("/api/ownership-entities/{id}/members/{userId}",
                        testEntity.getId(), testUser.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404_whenRemoveMemberNotFound() throws Exception {
        // act & assert
        mockMvc.perform(delete("/api/ownership-entities/{id}/members/{userId}",
                        testEntity.getId(), 9999L))
                .andExpect(status().isNotFound());
    }
}

