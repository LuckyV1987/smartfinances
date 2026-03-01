package com.smartfinances.service;

import com.smartfinances.entity.RefreshToken;
import com.smartfinances.entity.User;
import com.smartfinances.exception.ForbiddenException;
import com.smartfinances.exception.InvalidTokenException;
import com.smartfinances.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Generate new UUID token
        String tokenValue = UUID.randomUUID().toString();

        // Set expiry to 7 days from now
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpiration);

        // Create and persist
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(expiresAt)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validateAndRotate(String token) {
        // Look up token by value
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token is invalid or expired"));

        // Check if expired
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Revoke it
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Token is invalid or expired");
        }

        // Revoke the current token
        refreshTokenRepository.delete(refreshToken);

        // Create and return a new refresh token for the same user
        return createRefreshToken(refreshToken.getUser());
    }

    @Transactional
    public void revokeToken(String token, Long userId) {
        // Look up token - silently return if not found (idempotent)
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElse(null);

        if (refreshToken == null) {
            return;
        }

        // Verify token belongs to userId
        if (!refreshToken.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        // Revoke and save
        refreshTokenRepository.delete(refreshToken);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        // Find all tokens for userId
        List<RefreshToken> tokens = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .toList();

        // Delete all
        refreshTokenRepository.deleteAll(tokens);
    }
}

