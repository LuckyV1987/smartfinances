package com.smartfinances.service;

import com.smartfinances.dto.request.LoginRequestDTO;
import com.smartfinances.dto.response.TokenResponseDTO;
import com.smartfinances.entity.RefreshToken;
import com.smartfinances.entity.User;
import com.smartfinances.exception.AccountDeactivatedException;
import com.smartfinances.exception.InvalidCredentialsException;
import com.smartfinances.exception.InvalidTokenException;
import com.smartfinances.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public TokenResponseDTO login(LoginRequestDTO dto) {
        // Find user by email (including soft-deleted users for proper error messaging)
        User user = userRepository.findByEmailIncludingDeleted(dto.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Check user is not deleted
        if (user.getDeletedAt() != null) {
            throw new AccountDeactivatedException("Account is deactivated");
        }

        // Create refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Generate JWT
        String accessToken = jwtService.generateToken(user);

        // Return token response
        return TokenResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    @Transactional
    public TokenResponseDTO refresh(String refreshToken) {
        // Validate and rotate refresh token
        RefreshToken newRefreshToken = refreshTokenService.validateAndRotate(refreshToken);

        // Load user
        User user = newRefreshToken.getUser();

        // Generate new JWT
        String accessToken = jwtService.generateToken(user);

        // Return token response
        return TokenResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    @Transactional
    public void logout(String bearerToken, String refreshToken) {
        // Extract JWT from bearer token (remove "Bearer " prefix if present)
        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;

        // Validate JWT
        if (!jwtService.validateToken(token)) {
            throw new InvalidTokenException("Token is invalid or expired");
        }

        // Extract userId
        String userIdStr = jwtService.extractUserId(token);
        Long userId = Long.parseLong(userIdStr);

        // Revoke refresh token
        refreshTokenService.revokeToken(refreshToken, userId);
    }

    @Transactional
    public void logoutAll(String bearerToken) {
        // Extract JWT from bearer token (remove "Bearer " prefix if present)
        String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;

        // Validate JWT
        if (!jwtService.validateToken(token)) {
            throw new InvalidTokenException("Token is invalid or expired");
        }

        // Extract userId
        String userIdStr = jwtService.extractUserId(token);
        Long userId = Long.parseLong(userIdStr);

        // Revoke all tokens for user
        refreshTokenService.revokeAllForUser(userId);
    }
}

