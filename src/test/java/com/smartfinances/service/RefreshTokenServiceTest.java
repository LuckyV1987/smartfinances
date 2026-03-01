package com.smartfinances.service;

import com.smartfinances.entity.RefreshToken;
import com.smartfinances.entity.User;
import com.smartfinances.exception.ForbiddenException;
import com.smartfinances.exception.InvalidTokenException;
import com.smartfinances.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void shouldCreateRefreshToken_andRevokeExisting() {
        // arrange
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800L);

        User user = User.builder().id(1L).email("lucky@test.com").build();

        RefreshToken savedToken = RefreshToken.builder()
                .id(1L)
                .token("new-token-uuid")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

        // act
        RefreshToken result = refreshTokenService.createRefreshToken(user);

        // assert
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldRotateToken_whenValidToken() {
        // arrange
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800L);

        User user = User.builder().id(1L).email("lucky@test.com").build();

        RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .token("old-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        RefreshToken newToken = RefreshToken.builder()
                .id(2L)
                .token("new-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(newToken);

        // act
        RefreshToken result = refreshTokenService.validateAndRotate("old-token");

        // assert
        assertThat(result).isNotNull();
        verify(refreshTokenRepository).delete(oldToken);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldThrowException_whenTokenRevoked() {
        // arrange
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("revoked-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token is invalid or expired");
    }

    @Test
    void shouldThrowException_whenTokenExpired() {
        // arrange
        User user = User.builder().id(1L).build();

        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .user(user)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // act & assert
        assertThatThrownBy(() -> refreshTokenService.validateAndRotate("expired-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token is invalid or expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void shouldRevokeToken_whenBelongsToUser() {
        // arrange
        User user = User.builder().id(1L).build();

        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .token("token-to-revoke")
                .user(user)
                .build();

        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));

        // act
        refreshTokenService.revokeToken("token-to-revoke", 1L);

        // assert
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void shouldThrowForbidden_whenTokenBelongsToOtherUser() {
        // arrange
        User user = User.builder().id(1L).build();

        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .token("other-user-token")
                .user(user)
                .build();

        when(refreshTokenRepository.findByToken("other-user-token")).thenReturn(Optional.of(token));

        // act & assert
        assertThatThrownBy(() -> refreshTokenService.revokeToken("other-user-token", 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldRevokeAllTokens_forUser() {
        // arrange
        User user1 = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();

        RefreshToken token1 = RefreshToken.builder().id(1L).user(user1).build();
        RefreshToken token2 = RefreshToken.builder().id(2L).user(user1).build();
        RefreshToken token3 = RefreshToken.builder().id(3L).user(user2).build();

        when(refreshTokenRepository.findAll()).thenReturn(List.of(token1, token2, token3));

        // act
        refreshTokenService.revokeAllForUser(1L);

        // assert
        verify(refreshTokenRepository).deleteAll(List.of(token1, token2));
    }
}

