package com.smartfinances.repository;

import com.smartfinances.entity.RefreshToken;
import com.smartfinances.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RefreshTokenRepositoryIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindToken_whenTokenExists() {
        // arrange
        User user = User.builder()
                .email("user@test.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();
        userRepository.save(user);
        entityManager.flush();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token("test-token-123")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token);
        entityManager.flush();

        // act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("test-token-123");

        // assert
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("test-token-123");
        assertThat(found.get().getUser().getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void shouldReturnEmpty_whenTokenNotFound() {
        // arrange
        // no token created

        // act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent-token");

        // assert
        assertThat(found).isEmpty();
    }

    @Test
    void shouldDeleteTokens_whenUserProvided() {
        // arrange
        User user = User.builder()
                .email("user@test.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();
        userRepository.save(user);
        entityManager.flush();

        RefreshToken token1 = RefreshToken.builder()
                .user(user)
                .token("token-1")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        RefreshToken token2 = RefreshToken.builder()
                .user(user)
                .token("token-2")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        entityManager.flush();

        // act
        refreshTokenRepository.deleteByUser(user);

        // assert
        Optional<RefreshToken> found1 = refreshTokenRepository.findByToken("token-1");
        Optional<RefreshToken> found2 = refreshTokenRepository.findByToken("token-2");
        assertThat(found1).isEmpty();
        assertThat(found2).isEmpty();
    }

    @Test
    void shouldSaveToken_whenValidEntity() {
        // arrange
        User user = User.builder()
                .email("user@test.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();
        userRepository.save(user);
        entityManager.flush();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token("new-token-xyz")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        // act
        RefreshToken saved = refreshTokenRepository.save(token);
        entityManager.flush();

        // assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isEqualTo("new-token-xyz");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

}

