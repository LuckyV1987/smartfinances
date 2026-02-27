package com.smartfinances.repository;

import com.smartfinances.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindUser_whenEmailExists() {
        // arrange
        User user = User.builder()
                .email("lucky@test.com")
                .password("password123")
                .firstName("Lucky")
                .lastName("V")
                .phoneNumber("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        userRepository.save(user);
        entityManager.flush();

        // act
        Optional<User> found = userRepository.findByEmail("lucky@test.com");

        // assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("lucky@test.com");
        assertThat(found.get().getFirstName()).isEqualTo("Lucky");
        assertThat(found.get().getLastName()).isEqualTo("V");
    }

    @Test
    void shouldReturnEmpty_whenEmailNotFound() {
        // arrange
        // no user created

        // act
        Optional<User> found = userRepository.findByEmail("nonexistent@test.com");

        // assert
        assertThat(found).isEmpty();
    }

    @Test
    void shouldSaveUser_whenValidEntity() {
        // arrange
        User user = User.builder()
                .email("newuser@test.com")
                .password("securepassword")
                .firstName("New")
                .lastName("User")
                .build();

        // act
        User saved = userRepository.save(user);
        entityManager.flush();

        // assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("newuser@test.com");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldNotFindUser_whenSoftDeleted() {
        // arrange
        User user = User.builder()
                .email("deleted@test.com")
                .password("password123")
                .firstName("Deleted")
                .lastName("User")
                .deletedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        entityManager.flush();

        // act
        Optional<User> found = userRepository.findByEmail("deleted@test.com");

        // assert
        assertThat(found).isEmpty();
    }

}

