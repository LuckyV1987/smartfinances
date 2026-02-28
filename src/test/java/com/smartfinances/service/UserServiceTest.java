package com.smartfinances.service;

import com.smartfinances.dto.request.UserRequestDTO;
import com.smartfinances.dto.response.UserResponseDTO;
import com.smartfinances.entity.User;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.UserMapper;
import com.smartfinances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUser_whenValidRequest() {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("test@example.com")
                .password("Password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(requestDTO.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(requestDTO)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(responseDTO);

        // act
        UserResponseDTO result = userService.register(requestDTO);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(userRepository).findByEmail(requestDTO.getEmail());
        verify(userMapper).toEntity(requestDTO);
        verify(userRepository).save(any(User.class));
        verify(userMapper).toResponseDTO(user);
    }

    @Test
    void shouldThrowException_whenEmailAlreadyExists() {
        // arrange
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .email("existing@example.com")
                .password("Password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .email("existing@example.com")
                .build();

        when(userRepository.findByEmail(requestDTO.getEmail())).thenReturn(Optional.of(existingUser));

        // act & assert
        assertThatThrownBy(() -> userService.register(requestDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository).findByEmail(requestDTO.getEmail());
        verify(userMapper, never()).toEntity(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnUser_whenValidId() {
        // arrange
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(responseDTO);

        // act
        UserResponseDTO result = userService.findById(userId);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
        verify(userMapper).toResponseDTO(user);
    }

    @Test
    void shouldThrowException_whenUserNotFound() {
        // arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id");

        verify(userRepository).findById(userId);
        verify(userMapper, never()).toResponseDTO(any());
    }

    @Test
    void shouldThrowException_whenEmailNotFound() {
        // arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> userService.findByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email");

        verify(userRepository).findByEmail(email);
    }

}

