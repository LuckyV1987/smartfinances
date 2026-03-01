package com.smartfinances.service;

import com.smartfinances.dto.request.LoginRequestDTO;
import com.smartfinances.dto.response.TokenResponseDTO;
import com.smartfinances.entity.RefreshToken;
import com.smartfinances.entity.User;
import com.smartfinances.exception.AccountDeactivatedException;
import com.smartfinances.exception.InvalidCredentialsException;
import com.smartfinances.exception.InvalidTokenException;
import com.smartfinances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldReturnTokenResponse_whenValidCredentials() {
        // arrange
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .email("lucky@test.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(1L)
                .email("lucky@test.com")
                .password("hashedPassword")
                .deletedAt(null)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-uuid")
                .user(user)
                .build();

        when(userRepository.findByEmailIncludingDeleted("lucky@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);
        when(jwtService.generateToken(user)).thenReturn("jwt-access-token");

        // act
        TokenResponseDTO response = authService.login(dto);

        // assert
        assertThat(response.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-uuid");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);
        verify(refreshTokenService).createRefreshToken(user);
        verify(jwtService).generateToken(user);
    }

    @Test
    void shouldThrowInvalidCredentials_whenEmailNotFound() {
        // arrange
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .email("unknown@test.com")
                .password("password123")
                .build();

        when(userRepository.findByEmailIncludingDeleted("unknown@test.com")).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void shouldThrowInvalidCredentials_whenPasswordMismatch() {
        // arrange
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .email("lucky@test.com")
                .password("wrongPassword")
                .build();

        User user = User.builder()
                .id(1L)
                .email("lucky@test.com")
                .password("hashedPassword")
                .build();

        when(userRepository.findByEmailIncludingDeleted("lucky@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        // act & assert
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void shouldThrowAccountDeactivated_whenUserDeleted() {
        // arrange
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .email("lucky@test.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(1L)
                .email("lucky@test.com")
                .password("hashedPassword")
                .deletedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmailIncludingDeleted("lucky@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(AccountDeactivatedException.class)
                .hasMessage("Account is deactivated");
    }

    @Test
    void shouldReturnNewTokens_whenValidRefreshToken() {
        // arrange
        User user = User.builder()
                .id(1L)
                .email("lucky@test.com")
                .build();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token("new-refresh-token-uuid")
                .user(user)
                .build();

        when(refreshTokenService.validateAndRotate("old-refresh-token")).thenReturn(newRefreshToken);
        when(jwtService.generateToken(user)).thenReturn("new-jwt-access-token");

        // act
        TokenResponseDTO response = authService.refresh("old-refresh-token");

        // assert
        assertThat(response.getAccessToken()).isEqualTo("new-jwt-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token-uuid");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);
    }

    @Test
    void shouldThrowInvalidToken_whenRefreshTokenExpired() {
        // arrange
        when(refreshTokenService.validateAndRotate("expired-token"))
                .thenThrow(new InvalidTokenException("Token is invalid or expired"));

        // act & assert
        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token is invalid or expired");
    }

    @Test
    void shouldThrowInvalidToken_whenRefreshTokenRevoked() {
        // arrange
        when(refreshTokenService.validateAndRotate("revoked-token"))
                .thenThrow(new InvalidTokenException("Token is invalid or expired"));

        // act & assert
        assertThatThrownBy(() -> authService.refresh("revoked-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token is invalid or expired");
    }

    @Test
    void shouldRevokeToken_whenValidLogout() {
        // arrange
        String bearerToken = "Bearer valid-jwt-token";
        String refreshToken = "refresh-token-uuid";

        when(jwtService.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-jwt-token")).thenReturn("1");

        // act
        authService.logout(bearerToken, refreshToken);

        // assert
        verify(refreshTokenService).revokeToken("refresh-token-uuid", 1L);
    }

    @Test
    void shouldRevokeAllTokens_whenLogoutAll() {
        // arrange
        String bearerToken = "Bearer valid-jwt-token";

        when(jwtService.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-jwt-token")).thenReturn("1");

        // act
        authService.logoutAll(bearerToken);

        // assert
        verify(refreshTokenService).revokeAllForUser(1L);
    }
}

