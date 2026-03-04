package com.smartfinances.repository;

import com.smartfinances.entity.OwnershipMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnershipMembershipRepository extends JpaRepository<OwnershipMembership, Long> {

    // All active entities a user belongs to
    List<OwnershipMembership> findByUserIdAndActiveTrue(Long userId);

    // All active members of an entity
    List<OwnershipMembership> findByOwnershipEntityIdAndActiveTrue(Long ownershipEntityId);

    // Check if a specific user is already a member of an entity
    boolean existsByUserIdAndOwnershipEntityIdAndActiveTrue(Long userId, Long ownershipEntityId);

    // Find a specific membership record
    Optional<OwnershipMembership> findByUserIdAndOwnershipEntityIdAndActiveTrue(
        Long userId, Long ownershipEntityId
    );
}

