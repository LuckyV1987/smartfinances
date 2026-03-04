package com.smartfinances.controller;

import com.smartfinances.dto.request.MembershipRequestDTO;
import com.smartfinances.dto.request.OwnershipEntityRequestDTO;
import com.smartfinances.dto.request.OwnershipEntityUpdateRequestDTO;
import com.smartfinances.dto.response.MembershipResponseDTO;
import com.smartfinances.dto.response.OwnershipEntityResponseDTO;
import com.smartfinances.entity.enums.OwnershipEntityType;
import com.smartfinances.service.OwnershipEntityService;
import com.smartfinances.service.OwnershipMembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ownership-entities")
@Tag(name = "Ownership Entities")
public class OwnershipEntityController {

    private final OwnershipEntityService ownershipEntityService;
    private final OwnershipMembershipService ownershipMembershipService;

    public OwnershipEntityController(
            OwnershipEntityService ownershipEntityService,
            OwnershipMembershipService ownershipMembershipService) {
        this.ownershipEntityService = ownershipEntityService;
        this.ownershipMembershipService = ownershipMembershipService;
    }

    @GetMapping
    @Operation(summary = "Get all ownership entities", description = "Returns all active ownership entities, optionally filtered by type")
    public ResponseEntity<List<OwnershipEntityResponseDTO>> getAllEntities(
            @RequestParam(required = false) OwnershipEntityType type) {
        List<OwnershipEntityResponseDTO> entities;
        if (type != null) {
            entities = ownershipEntityService.findByType(type);
        } else {
            entities = ownershipEntityService.findAll();
        }
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ownership entity by ID", description = "Returns a single ownership entity by ID")
    public ResponseEntity<OwnershipEntityResponseDTO> getEntityById(@PathVariable Long id) {
        OwnershipEntityResponseDTO entity = ownershipEntityService.findById(id);
        return ResponseEntity.ok(entity);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get entities by user", description = "Returns all ownership entities the user is an active member of")
    public ResponseEntity<List<OwnershipEntityResponseDTO>> getEntitiesByUser(@PathVariable Long userId) {
        List<OwnershipEntityResponseDTO> entities = ownershipEntityService.findEntitiesByUser(userId);
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get members by entity", description = "Returns all active members of an ownership entity")
    public ResponseEntity<List<MembershipResponseDTO>> getMembersByEntity(@PathVariable Long id) {
        List<MembershipResponseDTO> members = ownershipMembershipService.findMembersByEntity(id);
        return ResponseEntity.ok(members);
    }

    @PostMapping
    @Operation(summary = "Create ownership entity", description = "Creates a new ownership entity")
    public ResponseEntity<OwnershipEntityResponseDTO> createEntity(
            @Valid @RequestBody OwnershipEntityRequestDTO dto) {
        OwnershipEntityResponseDTO entity = ownershipEntityService.create(dto);
        return new ResponseEntity<>(entity, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ownership entity", description = "Updates an existing ownership entity")
    public ResponseEntity<OwnershipEntityResponseDTO> updateEntity(
            @PathVariable Long id,
            @Valid @RequestBody OwnershipEntityUpdateRequestDTO dto) {
        OwnershipEntityResponseDTO entity = ownershipEntityService.update(id, dto);
        return ResponseEntity.ok(entity);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate ownership entity", description = "Soft deletes an ownership entity by setting active to false")
    public ResponseEntity<Void> deactivateEntity(@PathVariable Long id) {
        ownershipEntityService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to entity", description = "Adds a user as a member of an ownership entity")
    public ResponseEntity<MembershipResponseDTO> addMember(
            @PathVariable Long id,
            @Valid @RequestBody MembershipRequestDTO dto) {
        MembershipResponseDTO membership = ownershipMembershipService.addMember(id, dto);
        return new ResponseEntity<>(membership, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member from entity", description = "Soft deletes a membership by setting active to false")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        ownershipMembershipService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}

