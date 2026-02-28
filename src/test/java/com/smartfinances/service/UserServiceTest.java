package com.smartfinances.service;

import com.smartfinances.dto.request.UserRequestDTO;
import com.smartfinances.dto.request.UserUpdateRequestDTO;
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

import java.time.LocalDate;
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

    @Test
    void shouldUpdateUser_whenValidRequest() {
        // arrange
        Long userId = 1L;
        UserUpdateRequestDTO updateDTO = UserUpdateRequestDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("9876543210")
                .build();

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        User updatedUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("9876543210")
                .build();

        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("9876543210")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(userMapper.toResponseDTO(updatedUser)).thenReturn(responseDTO);

        // act
        UserResponseDTO result = userService.update(userId, updateDTO);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        verify(userRepository).findById(userId);
        verify(userMapper).updateEntityFromDTO(updateDTO, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponseDTO(updatedUser);
    }

    @Test
    void shouldThrowException_whenUserNotFoundOnUpdate() {
        // arrange
        Long userId = 999L;
        UserUpdateRequestDTO updateDTO = UserUpdateRequestDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> userService.update(userId, updateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id");

        verify(userRepository).findById(userId);
        verify(userMapper, never()).updateEntityFromDTO(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateUser_whenValidId() {
        // arrange
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // act
        userService.deactivate(userId);

        // assert
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldThrowException_whenUserNotFoundOnDeactivate() {
        // arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> userService.deactivate(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldOnlyUpdateProvidedFields_whenPartialRequest() {
        // arrange
        Long userId = 1L;
        UserUpdateRequestDTO updateDTO = UserUpdateRequestDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        User updatedUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        UserResponseDTO responseDTO = UserResponseDTO.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("1234567890")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(updatedUser);
        when(userMapper.toResponseDTO(updatedUser)).thenReturn(responseDTO);

        // act
        UserResponseDTO result = userService.update(userId, updateDTO);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getPhoneNumber()).isEqualTo("1234567890");
        assertThat(result.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        verify(userRepository).findById(userId);
        verify(userMapper).updateEntityFromDTO(updateDTO, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponseDTO(updatedUser);
    }

    @Test
    void shouldNotReturnDeactivatedUser_whenDeleted() {
        // arrange
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .deletedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id");

        verify(userRepository).findById(userId);
    }

}
