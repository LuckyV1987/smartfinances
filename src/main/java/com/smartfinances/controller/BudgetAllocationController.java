package com.smartfinances.controller;

import com.smartfinances.dto.request.BudgetAllocationRequestDTO;
import com.smartfinances.dto.request.BudgetAllocationUpdateRequestDTO;
import com.smartfinances.dto.request.CategoryLinkRequestDTO;
import com.smartfinances.dto.response.BudgetAllocationResponseDTO;
import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.enums.AllocationTypeEnum;
import com.smartfinances.service.BudgetAllocationCategoryService;
import com.smartfinances.service.BudgetAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget-allocations")
@Tag(name = "Budget Allocations", description = "Budget allocation management operations")
public class BudgetAllocationController {

    private final BudgetAllocationService budgetAllocationService;
    private final BudgetAllocationCategoryService budgetAllocationCategoryService;

    public BudgetAllocationController(
            BudgetAllocationService budgetAllocationService,
            BudgetAllocationCategoryService budgetAllocationCategoryService) {
        this.budgetAllocationService = budgetAllocationService;
        this.budgetAllocationCategoryService = budgetAllocationCategoryService;
    }

    @GetMapping
    @Operation(summary = "Get all budget allocations", description = "Returns all active budget allocations for an ownership entity, optionally filtered by type")
    public ResponseEntity<List<BudgetAllocationResponseDTO>> getAllAllocations(
            @RequestParam Long ownershipEntityId,
            @RequestParam(required = false) AllocationTypeEnum type) {
        List<BudgetAllocationResponseDTO> allocations;
        if (type != null) {
            allocations = budgetAllocationService.findAllByEntityAndType(ownershipEntityId, type);
        } else {
            allocations = budgetAllocationService.findAllByEntity(ownershipEntityId);
        }
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget allocation by ID", description = "Returns a single budget allocation by ID")
    public ResponseEntity<BudgetAllocationResponseDTO> getAllocationById(@PathVariable Long id) {
        BudgetAllocationResponseDTO allocation = budgetAllocationService.findById(id);
        return ResponseEntity.ok(allocation);
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Check allocation balance", description = "Forces fresh balance calculation for an allocation")
    public ResponseEntity<BudgetAllocationResponseDTO> checkBalance(@PathVariable Long id) {
        BudgetAllocationResponseDTO allocation = budgetAllocationService.checkBalance(id);
        return ResponseEntity.ok(allocation);
    }

    @PostMapping
    @Operation(summary = "Create budget allocation", description = "Creates a new budget allocation")
    public ResponseEntity<BudgetAllocationResponseDTO> createAllocation(
            @Valid @RequestBody BudgetAllocationRequestDTO dto) {
        BudgetAllocationResponseDTO allocation = budgetAllocationService.create(dto);
        return new ResponseEntity<>(allocation, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget allocation", description = "Updates an existing budget allocation")
    public ResponseEntity<BudgetAllocationResponseDTO> updateAllocation(
            @PathVariable Long id,
            @Valid @RequestBody BudgetAllocationUpdateRequestDTO dto) {
        BudgetAllocationResponseDTO allocation = budgetAllocationService.update(id, dto);
        return ResponseEntity.ok(allocation);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate budget allocation", description = "Soft deletes a budget allocation by setting active to false")
    public ResponseEntity<Void> deactivateAllocation(@PathVariable Long id) {
        budgetAllocationService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/categories")
    @Operation(summary = "Link category to allocation", description = "Links a spend category to a budget allocation")
    public ResponseEntity<List<SpendCategoryResponseDTO>> linkCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryLinkRequestDTO dto) {
        List<SpendCategoryResponseDTO> categories = budgetAllocationCategoryService.linkCategory(id, dto.getSpendCategoryId());
        return ResponseEntity.ok(categories);
    }

    @DeleteMapping("/{id}/categories/{categoryId}")
    @Operation(summary = "Unlink category from allocation", description = "Removes a spend category link from a budget allocation")
    public ResponseEntity<List<SpendCategoryResponseDTO>> unlinkCategory(
            @PathVariable Long id,
            @PathVariable Long categoryId) {
        List<SpendCategoryResponseDTO> categories = budgetAllocationCategoryService.unlinkCategory(id, categoryId);
        return ResponseEntity.ok(categories);
    }
}

