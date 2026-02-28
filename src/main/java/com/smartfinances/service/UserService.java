package com.smartfinances.service;

import com.smartfinances.dto.request.UserRequestDTO;
import com.smartfinances.dto.request.UserUpdateRequestDTO;
import com.smartfinances.dto.response.UserResponseDTO;
import com.smartfinances.entity.User;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.UserMapper;
import com.smartfinances.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponseDTO register(UserRequestDTO dto) {
        // Check if email already exists
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists: " + dto.getEmail());
        }

        // Map DTO to entity
        User user = userMapper.toEntity(dto);

        // Save entity
        User savedUser = userRepository.save(user);

        // Return response DTO
        return userMapper.toResponseDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponseDTO(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public UserResponseDTO update(Long id, UserUpdateRequestDTO dto) {
        // Find user by id
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Update fields
        userMapper.updateEntityFromDTO(dto, user);

        // Save and return
        User updatedUser = userRepository.save(user);
        return userMapper.toResponseDTO(updatedUser);
    }

    @Transactional
    public void deactivate(Long id) {
        // Find user by id
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Set deletedAt
        user.setDeletedAt(LocalDateTime.now());

        // Save
        userRepository.save(user);
    }

}

