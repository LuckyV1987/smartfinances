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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OwnershipEntityService {

    private final OwnershipEntityRepository ownershipEntityRepository;
    private final OwnershipMembershipRepository ownershipMembershipRepository;
    private final OwnershipEntityMapper ownershipEntityMapper;

    public OwnershipEntityService(
            OwnershipEntityRepository ownershipEntityRepository,
            OwnershipMembershipRepository ownershipMembershipRepository,
            OwnershipEntityMapper ownershipEntityMapper) {
        this.ownershipEntityRepository = ownershipEntityRepository;
        this.ownershipMembershipRepository = ownershipMembershipRepository;
        this.ownershipEntityMapper = ownershipEntityMapper;
    }

    @Transactional(readOnly = true)
    public List<OwnershipEntityResponseDTO> findAll() {
        return ownershipEntityRepository.findByActiveTrue().stream()
                .map(ownershipEntityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OwnershipEntityResponseDTO findById(Long id) {
        OwnershipEntity entity = ownershipEntityRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + id));
        return ownershipEntityMapper.toResponseDTO(entity);
    }

    @Transactional(readOnly = true)
    public List<OwnershipEntityResponseDTO> findByType(OwnershipEntityType type) {
        return ownershipEntityRepository.findByTypeAndActiveTrue(type).stream()
                .map(ownershipEntityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OwnershipEntityResponseDTO> findEntitiesByUser(Long userId) {
        List<OwnershipMembership> memberships = ownershipMembershipRepository.findByUserIdAndActiveTrue(userId);
        return memberships.stream()
                .map(membership -> ownershipEntityMapper.toResponseDTO(membership.getOwnershipEntity()))
                .collect(Collectors.toList());
    }


    @Transactional
    public OwnershipEntityResponseDTO create(OwnershipEntityRequestDTO dto) {
        OwnershipEntity entity = ownershipEntityMapper.toEntity(dto);
        OwnershipEntity savedEntity = ownershipEntityRepository.save(entity);
        return ownershipEntityMapper.toResponseDTO(savedEntity);
    }

    @Transactional
    public OwnershipEntity createPersonalOwnershipEntity(User user) {
        String personalEntityName = user.getFirstName() + "'s Personal Space";
        OwnershipEntity entity = OwnershipEntity.builder()
                .name(personalEntityName)
                .type(OwnershipEntityType.PERSONAL)
                .active(true)
                .build();
        return ownershipEntityRepository.save(entity);
    }

    @Transactional
    public OwnershipEntityResponseDTO update(Long id, OwnershipEntityUpdateRequestDTO dto) {
        OwnershipEntity entity = ownershipEntityRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + id));

        ownershipEntityMapper.updateEntityFromDTO(dto, entity);
        OwnershipEntity updatedEntity = ownershipEntityRepository.save(entity);
        return ownershipEntityMapper.toResponseDTO(updatedEntity);
    }

    @Transactional
    public void deactivate(Long id) {
        OwnershipEntity entity = ownershipEntityRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + id));

        entity.setActive(false);
        ownershipEntityRepository.save(entity);
    }
}

