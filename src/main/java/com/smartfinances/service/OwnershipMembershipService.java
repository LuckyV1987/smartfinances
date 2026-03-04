package com.smartfinances.service;

import com.smartfinances.dto.request.MembershipRequestDTO;
import com.smartfinances.dto.response.MembershipResponseDTO;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.OwnershipMembership;
import com.smartfinances.entity.User;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.OwnershipEntityMapper;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.OwnershipMembershipRepository;
import com.smartfinances.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OwnershipMembershipService {

    private final OwnershipMembershipRepository ownershipMembershipRepository;
    private final OwnershipEntityRepository ownershipEntityRepository;
    private final UserRepository userRepository;
    private final OwnershipEntityMapper ownershipEntityMapper;

    public OwnershipMembershipService(
            OwnershipMembershipRepository ownershipMembershipRepository,
            OwnershipEntityRepository ownershipEntityRepository,
            UserRepository userRepository,
            OwnershipEntityMapper ownershipEntityMapper) {
        this.ownershipMembershipRepository = ownershipMembershipRepository;
        this.ownershipEntityRepository = ownershipEntityRepository;
        this.userRepository = userRepository;
        this.ownershipEntityMapper = ownershipEntityMapper;
    }

    @Transactional(readOnly = true)
    public List<MembershipResponseDTO> findMembersByEntity(Long ownershipEntityId) {
        // Verify entity exists and is active
        ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        List<OwnershipMembership> memberships = ownershipMembershipRepository.findByOwnershipEntityIdAndActiveTrue(ownershipEntityId);
        return memberships.stream()
                .map(ownershipEntityMapper::toMembershipResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public OwnershipMembership createMembership(OwnershipEntity entity, User user) {
        // Check if active membership already exists
        if (ownershipMembershipRepository.existsByUserIdAndOwnershipEntityIdAndActiveTrue(user.getId(), entity.getId())) {
            throw new DuplicateResourceException("User is already a member of this ownership entity");
        }

        OwnershipMembership membership = OwnershipMembership.builder()
                .user(user)
                .ownershipEntity(entity)
                .active(true)
                .build();

        return ownershipMembershipRepository.save(membership);
    }

    @Transactional
    public MembershipResponseDTO addMember(Long ownershipEntityId, MembershipRequestDTO dto) {
        // Find entity
        OwnershipEntity entity = ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        // Find user
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));

        // Create membership
        OwnershipMembership membership = createMembership(entity, user);

        return ownershipEntityMapper.toMembershipResponseDTO(membership);
    }

    @Transactional
    public void removeMember(Long ownershipEntityId, Long userId) {
        // Find entity
        ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        // Find active membership
        OwnershipMembership membership = ownershipMembershipRepository
                .findByUserIdAndOwnershipEntityIdAndActiveTrue(userId, ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found for user id: " + userId + " and ownership entity id: " + ownershipEntityId));

        // Soft delete
        membership.setActive(false);
        ownershipMembershipRepository.save(membership);
    }
}

