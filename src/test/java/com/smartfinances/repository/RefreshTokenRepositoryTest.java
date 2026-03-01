package com.smartfinances.repository;

import com.smartfinances.SmartFinancesApplication;
import com.smartfinances.entity.RefreshToken;
import com.smartfinances.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SmartFinancesApplication.class)
@Transactional
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindToken_whenValidTokenString() {
        // arrange
        User user = User.builder()
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        RefreshToken token = RefreshToken.builder()
                .token("test-token-uuid")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token);

        // act
        Optional<RefreshToken> result = refreshTokenRepository.findByToken("test-token-uuid");

        // assert
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("test-token-uuid");
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void shouldReturnEmpty_whenTokenNotFound() {
        // act
        Optional<RefreshToken> result = refreshTokenRepository.findByToken("non-existent-token");

        // assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteByUser_whenUserHasTokens() {
        // arrange
        User user = User.builder()
                .email("user@example.com")
                .password("hashedPassword")
                .firstName("Jane")
                .lastName("Doe")
                .build();
        userRepository.save(user);

        RefreshToken token1 = RefreshToken.builder()
                .token("token-1")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        RefreshToken token2 = RefreshToken.builder()
                .token("token-2")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        // act
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        // assert
        assertThat(refreshTokenRepository.findByToken("token-1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("token-2")).isEmpty();
    }

    @Test
    void shouldDeleteByUserId_whenUserHasMultipleTokens() {
        // arrange
        User user = User.builder()
                .email("multi@example.com")
                .password("hashedPassword")
                .firstName("Multi")
                .lastName("Token")
                .build();
        userRepository.save(user);

        RefreshToken token1 = RefreshToken.builder()
                .token("token-a")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        RefreshToken token2 = RefreshToken.builder()
                .token("token-b")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        RefreshToken token3 = RefreshToken.builder()
                .token("token-c")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        refreshTokenRepository.save(token3);

        // act
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.flush();

        // assert
        assertThat(refreshTokenRepository.findByToken("token-a")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("token-b")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("token-c")).isEmpty();
    }

    @Test
    void shouldNotDeleteOtherUserTokens_whenDeletingByUserId() {
        // arrange
        User user1 = User.builder()
                .email("user1@example.com")
                .password("hashedPassword")
                .firstName("User")
                .lastName("One")
                .build();
        User user2 = User.builder()
                .email("user2@example.com")
                .password("hashedPassword")
                .firstName("User")
                .lastName("Two")
                .build();
        userRepository.save(user1);
        userRepository.save(user2);

        RefreshToken user1Token = RefreshToken.builder()
                .token("user1-token")
                .user(user1)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        RefreshToken user2Token = RefreshToken.builder()
                .token("user2-token")
                .user(user2)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(user1Token);
        refreshTokenRepository.save(user2Token);

        // act
        refreshTokenRepository.deleteByUserId(user1.getId());
        refreshTokenRepository.flush();

        // assert
        assertThat(refreshTokenRepository.findByToken("user1-token")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("user2-token")).isPresent();
    }
}

