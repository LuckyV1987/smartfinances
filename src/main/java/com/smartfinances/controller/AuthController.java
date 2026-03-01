package com.smartfinances.controller;

import com.smartfinances.dto.request.LoginRequestDTO;
import com.smartfinances.dto.request.LogoutRequestDTO;
import com.smartfinances.dto.request.RefreshRequestDTO;
import com.smartfinances.dto.request.UserRequestDTO;
import com.smartfinances.dto.response.TokenResponseDTO;
import com.smartfinances.dto.response.UserResponseDTO;
import com.smartfinances.service.AuthService;
import com.smartfinances.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration and authentication")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO response = userService.register(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates user and returns JWT tokens")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        TokenResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generates new access token using refresh token")
    public ResponseEntity<TokenResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        TokenResponseDTO response = authService.refresh(dto.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the specified refresh token")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody LogoutRequestDTO dto) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new com.smartfinances.exception.InvalidCredentialsException("Authorization header is required");
        }
        authService.logout(authHeader, dto.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all sessions", description = "Revokes all refresh tokens for the user")
    public ResponseEntity<Void> logoutAll(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new com.smartfinances.exception.InvalidCredentialsException("Authorization header is required");
        }
        authService.logoutAll(authHeader);
        return ResponseEntity.noContent().build();
    }

}

