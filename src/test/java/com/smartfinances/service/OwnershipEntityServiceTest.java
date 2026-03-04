package com.smartfinances.service;

import com.smartfinances.dto.request.OwnershipEntityRequestDTO;
import com.smartfinances.dto.request.OwnershipEntityUpdateRequestDTO;
import com.smartfinances.dto.response.OwnershipEntityResponseDTO;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.OwnershipMembership;
import com.smartfinances.entity.User;
import com.smartfinances.entity.enums.OwnershipEntityType;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.OwnershipEntityMapper;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.OwnershipMembershipRepository;
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
class OwnershipEntityServiceTest {

    @Mock
    private OwnershipEntityRepository ownershipEntityRepository;

    @Mock
    private OwnershipMembershipRepository ownershipMembershipRepository;

    @Mock
    private OwnershipEntityMapper ownershipEntityMapper;

    @InjectMocks
    private OwnershipEntityService ownershipEntityService;

    @Test
    void shouldCreatePersonalOwnershipEntity_whenNewUserRegisters() {
        // arrange
        User user = User.builder()
                .id(1L)
                .firstName("Lucky")
                .build();

        OwnershipEntity savedEntity = OwnershipEntity.builder()
                .id(1L)
                .name("Lucky's Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        when(ownershipEntityRepository.save(any(OwnershipEntity.class))).thenReturn(savedEntity);

        // act
        OwnershipEntity result = ownershipEntityService.createPersonalOwnershipEntity(user);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Lucky's Personal Space");
        assertThat(result.getType()).isEqualTo(OwnershipEntityType.PERSONAL);
        assertThat(result.isActive()).isTrue();
        verify(ownershipEntityRepository).save(any(OwnershipEntity.class));
    }

    @Test
    void shouldReturnAllActiveEntities_whenFindAll() {
        // arrange
        OwnershipEntity entity1 = OwnershipEntity.builder()
                .id(1L)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        OwnershipEntity entity2 = OwnershipEntity.builder()
                .id(2L)
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();

        OwnershipEntityResponseDTO dto1 = OwnershipEntityResponseDTO.builder()
                .id(1L)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        OwnershipEntityResponseDTO dto2 = OwnershipEntityResponseDTO.builder()
                .id(2L)
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByActiveTrue()).thenReturn(Arrays.asList(entity1, entity2));
        when(ownershipEntityMapper.toResponseDTO(entity1)).thenReturn(dto1);
        when(ownershipEntityMapper.toResponseDTO(entity2)).thenReturn(dto2);

        // act
        List<OwnershipEntityResponseDTO> result = ownershipEntityService.findAll();

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Personal Space");
        assertThat(result.get(1).getName()).isEqualTo("Family Household");
        verify(ownershipEntityRepository).findByActiveTrue();
    }

    @Test
    void shouldReturnEntitiesByType_whenFindByType() {
        // arrange
        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        OwnershipEntityResponseDTO dto = OwnershipEntityResponseDTO.builder()
                .id(1L)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByTypeAndActiveTrue(OwnershipEntityType.PERSONAL))
                .thenReturn(Arrays.asList(entity));
        when(ownershipEntityMapper.toResponseDTO(entity)).thenReturn(dto);

        // act
        List<OwnershipEntityResponseDTO> result = ownershipEntityService.findByType(OwnershipEntityType.PERSONAL);

        // assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(OwnershipEntityType.PERSONAL);
        verify(ownershipEntityRepository).findByTypeAndActiveTrue(OwnershipEntityType.PERSONAL);
    }

    @Test
    void shouldReturnEntity_whenValidId() {
        // arrange
        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        OwnershipEntityResponseDTO dto = OwnershipEntityResponseDTO.builder()
                .id(1L)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(ownershipEntityMapper.toResponseDTO(entity)).thenReturn(dto);

        // act
        OwnershipEntityResponseDTO result = ownershipEntityService.findById(1L);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Personal Space");
        verify(ownershipEntityRepository).findByIdAndActiveTrue(1L);
    }

    @Test
    void shouldThrowException_whenEntityNotFound() {
        // arrange
        when(ownershipEntityRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> ownershipEntityService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ownership entity not found with id: 99");
    }

    @Test
    void shouldThrowException_whenEntityInactive() {
        // arrange
        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> ownershipEntityService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnEntities_whenFindByUser() {
        // arrange
        Long userId = 1L;

        OwnershipEntity entity1 = OwnershipEntity.builder()
                .id(1L)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        OwnershipEntity entity2 = OwnershipEntity.builder()
                .id(2L)
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();

        OwnershipMembership membership1 = OwnershipMembership.builder()
                .id(1L)
                .ownershipEntity(entity1)
                .active(true)
                .build();

        OwnershipMembership membership2 = OwnershipMembership.builder()
                .id(2L)
                .ownershipEntity(entity2)
                .active(true)
                .build();

        OwnershipEntityResponseDTO dto1 = OwnershipEntityResponseDTO.builder()
                .id(1L)
                .name("Personal Space")
                .build();

        OwnershipEntityResponseDTO dto2 = OwnershipEntityResponseDTO.builder()
                .id(2L)
                .name("Family Household")
                .build();

        when(ownershipMembershipRepository.findByUserIdAndActiveTrue(userId))
                .thenReturn(Arrays.asList(membership1, membership2));
        when(ownershipEntityMapper.toResponseDTO(entity1)).thenReturn(dto1);
        when(ownershipEntityMapper.toResponseDTO(entity2)).thenReturn(dto2);

        // act
        List<OwnershipEntityResponseDTO> result = ownershipEntityService.findEntitiesByUser(userId);

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Personal Space");
        assertThat(result.get(1).getName()).isEqualTo("Family Household");
        verify(ownershipMembershipRepository).findByUserIdAndActiveTrue(userId);
    }


    @Test
    void shouldCreateEntity_whenValidRequest() {
        // arrange
        OwnershipEntityRequestDTO request = OwnershipEntityRequestDTO.builder()
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("Our family budget")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("Our family budget")
                .active(true)
                .build();

        OwnershipEntity savedEntity = OwnershipEntity.builder()
                .id(1L)
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("Our family budget")
                .active(true)
                .build();

        OwnershipEntityResponseDTO response = OwnershipEntityResponseDTO.builder()
                .id(1L)
                .name("Family Household")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("Our family budget")
                .active(true)
                .build();

        when(ownershipEntityMapper.toEntity(request)).thenReturn(entity);
        when(ownershipEntityRepository.save(entity)).thenReturn(savedEntity);
        when(ownershipEntityMapper.toResponseDTO(savedEntity)).thenReturn(response);

        // act
        OwnershipEntityResponseDTO result = ownershipEntityService.create(request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Family Household");
        assertThat(result.getType()).isEqualTo(OwnershipEntityType.HOUSEHOLD);
        verify(ownershipEntityRepository).save(entity);
    }

    @Test
    void shouldUpdateEntity_whenValidRequest() {
        // arrange
        Long entityId = 1L;
        OwnershipEntityUpdateRequestDTO request = OwnershipEntityUpdateRequestDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Old Name")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("Old description")
                .active(true)
                .build();

        OwnershipEntity updatedEntity = OwnershipEntity.builder()
                .id(entityId)
                .name("Updated Name")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("Updated description")
                .active(true)
                .build();

        OwnershipEntityResponseDTO response = OwnershipEntityResponseDTO.builder()
                .id(entityId)
                .name("Updated Name")
                .type(OwnershipEntityType.HOUSEHOLD)
                .description("Updated description")
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        doNothing().when(ownershipEntityMapper).updateEntityFromDTO(request, entity);
        when(ownershipEntityRepository.save(entity)).thenReturn(updatedEntity);
        when(ownershipEntityMapper.toResponseDTO(updatedEntity)).thenReturn(response);

        // act
        OwnershipEntityResponseDTO result = ownershipEntityService.update(entityId, request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        verify(ownershipEntityMapper).updateEntityFromDTO(request, entity);
        verify(ownershipEntityRepository).save(entity);
    }

    @Test
    void shouldNotUpdateType_whenTypeProvidedInUpdate() {
        // arrange
        Long entityId = 1L;
        OwnershipEntityUpdateRequestDTO request = OwnershipEntityUpdateRequestDTO.builder()
                .name("Updated Name")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Old Name")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();

        OwnershipEntity savedEntity = OwnershipEntity.builder()
                .id(entityId)
                .name("Updated Name")
                .type(OwnershipEntityType.HOUSEHOLD) // Type should remain unchanged
                .active(true)
                .build();

        OwnershipEntityResponseDTO response = OwnershipEntityResponseDTO.builder()
                .id(entityId)
                .name("Updated Name")
                .type(OwnershipEntityType.HOUSEHOLD)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        doNothing().when(ownershipEntityMapper).updateEntityFromDTO(request, entity);
        when(ownershipEntityRepository.save(entity)).thenReturn(savedEntity);
        when(ownershipEntityMapper.toResponseDTO(savedEntity)).thenReturn(response);

        // act
        OwnershipEntityResponseDTO result = ownershipEntityService.update(entityId, request);

        // assert
        assertThat(result.getType()).isEqualTo(OwnershipEntityType.HOUSEHOLD);
        verify(ownershipEntityMapper).updateEntityFromDTO(request, entity);
    }

    @Test
    void shouldDeactivateEntity_whenValidId() {
        // arrange
        Long entityId = 1L;
        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Personal Space")
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(ownershipEntityRepository.save(any(OwnershipEntity.class))).thenReturn(entity);

        // act
        ownershipEntityService.deactivate(entityId);

        // assert
        verify(ownershipEntityRepository).findByIdAndActiveTrue(entityId);
        verify(ownershipEntityRepository).save(entity);
        assertThat(entity.isActive()).isFalse();
    }
}

