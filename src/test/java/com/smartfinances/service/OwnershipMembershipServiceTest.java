package com.smartfinances.service;

import com.smartfinances.dto.request.MembershipRequestDTO;
import com.smartfinances.dto.response.MembershipResponseDTO;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.OwnershipMembership;
import com.smartfinances.entity.User;
import com.smartfinances.entity.enums.OwnershipEntityType;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.OwnershipEntityMapper;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.OwnershipMembershipRepository;
import com.smartfinances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnershipMembershipServiceTest {

    @Mock
    private OwnershipMembershipRepository ownershipMembershipRepository;

    @Mock
    private OwnershipEntityRepository ownershipEntityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OwnershipEntityMapper ownershipEntityMapper;

    @InjectMocks
    private OwnershipMembershipService ownershipMembershipService;

    @Test
    void shouldReturnMembers_whenFindByEntity() {
        // arrange
        Long entityId = 1L;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Family Household")
                .active(true)
                .build();

        User user1 = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();

        OwnershipMembership membership1 = OwnershipMembership.builder()
                .id(1L)
                .user(user1)
                .ownershipEntity(entity)
                .active(true)
                .build();

        OwnershipMembership membership2 = OwnershipMembership.builder()
                .id(2L)
                .user(user2)
                .ownershipEntity(entity)
                .active(true)
                .build();

        MembershipResponseDTO dto1 = MembershipResponseDTO.builder()
                .id(1L)
                .userId(1L)
                .build();

        MembershipResponseDTO dto2 = MembershipResponseDTO.builder()
                .id(2L)
                .userId(2L)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(ownershipMembershipRepository.findByOwnershipEntityIdAndActiveTrue(entityId))
                .thenReturn(Arrays.asList(membership1, membership2));
        when(ownershipEntityMapper.toMembershipResponseDTO(membership1)).thenReturn(dto1);
        when(ownershipEntityMapper.toMembershipResponseDTO(membership2)).thenReturn(dto2);

        // act
        List<MembershipResponseDTO> result = ownershipMembershipService.findMembersByEntity(entityId);

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        assertThat(result.get(1).getUserId()).isEqualTo(2L);
        verify(ownershipEntityRepository).findByIdAndActiveTrue(entityId);
        verify(ownershipMembershipRepository).findByOwnershipEntityIdAndActiveTrue(entityId);
    }

    @Test
    void shouldThrowException_whenEntityNotFoundOnFindMembers() {
        // arrange
        Long entityId = 99L;
        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> ownershipMembershipService.findMembersByEntity(entityId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ownership entity not found");

        verify(ownershipEntityRepository).findByIdAndActiveTrue(entityId);
        verify(ownershipMembershipRepository, never()).findByOwnershipEntityIdAndActiveTrue(anyLong());
    }

    @Test
    void shouldAddMember_whenValidRequest() {
        // arrange
        Long entityId = 1L;
        Long userId = 2L;

        MembershipRequestDTO request = MembershipRequestDTO.builder()
                .userId(userId)
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();

        User user = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .build();

        OwnershipMembership savedMembership = OwnershipMembership.builder()
                .id(1L)
                .user(user)
                .ownershipEntity(entity)
                .active(true)
                .build();

        MembershipResponseDTO response = MembershipResponseDTO.builder()
                .id(1L)
                .userId(userId)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ownershipMembershipRepository.existsByUserIdAndOwnershipEntityIdAndActiveTrue(userId, entityId))
                .thenReturn(false);
        when(ownershipMembershipRepository.save(any(OwnershipMembership.class))).thenReturn(savedMembership);
        when(ownershipEntityMapper.toMembershipResponseDTO(savedMembership)).thenReturn(response);

        // act
        MembershipResponseDTO result = ownershipMembershipService.addMember(entityId, request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(ownershipMembershipRepository).save(any(OwnershipMembership.class));
    }

    @Test
    void shouldThrowException_whenMemberAlreadyExists() {
        // arrange
        Long entityId = 1L;
        Long userId = 2L;

        MembershipRequestDTO request = MembershipRequestDTO.builder()
                .userId(userId)
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Family Household")
                .active(true)
                .build();

        User user = User.builder()
                .id(userId)
                .firstName("John")
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ownershipMembershipRepository.existsByUserIdAndOwnershipEntityIdAndActiveTrue(userId, entityId))
                .thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> ownershipMembershipService.addMember(entityId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User is already a member of this ownership entity");

        verify(ownershipMembershipRepository, never()).save(any(OwnershipMembership.class));
    }

    @Test
    void shouldRemoveMember_whenValidRequest() {
        // arrange
        Long entityId = 1L;
        Long userId = 2L;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Family Household")
                .active(true)
                .build();

        User user = User.builder()
                .id(userId)
                .build();

        OwnershipMembership membership = OwnershipMembership.builder()
                .id(1L)
                .user(user)
                .ownershipEntity(entity)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(ownershipMembershipRepository.findByUserIdAndOwnershipEntityIdAndActiveTrue(userId, entityId))
                .thenReturn(Optional.of(membership));
        when(ownershipMembershipRepository.save(membership)).thenReturn(membership);

        // act
        ownershipMembershipService.removeMember(entityId, userId);

        // assert
        assertThat(membership.isActive()).isFalse();
        verify(ownershipMembershipRepository).save(membership);
    }

    @Test
    void shouldThrowException_whenMembershipNotFound() {
        // arrange
        Long entityId = 1L;
        Long userId = 2L;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Family Household")
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(ownershipMembershipRepository.findByUserIdAndOwnershipEntityIdAndActiveTrue(userId, entityId))
                .thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> ownershipMembershipService.removeMember(entityId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Membership not found");
    }
}

