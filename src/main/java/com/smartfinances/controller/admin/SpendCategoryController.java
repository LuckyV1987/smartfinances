package com.smartfinances.controller.admin;

import com.smartfinances.dto.request.SpendCategoryRequestDTO;
import com.smartfinances.dto.response.SpendCategoryResponseDTO;
import com.smartfinances.entity.enums.SpendCategoryType;
import com.smartfinances.service.SpendCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/spend-categories")
@Tag(name = "Admin - Spend Categories", description = "Manage system spend categories")
public class SpendCategoryController {

    private final SpendCategoryService spendCategoryService;

    public SpendCategoryController(SpendCategoryService spendCategoryService) {
        this.spendCategoryService = spendCategoryService;
    }

    @GetMapping
    @Operation(summary = "Get all spend categories", description = "Returns all active spend categories, optionally filtered by type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved categories")
    })
    public ResponseEntity<List<SpendCategoryResponseDTO>> getAllCategories(
            @RequestParam(required = false) SpendCategoryType type) {
        List<SpendCategoryResponseDTO> categories = type == null
                ? spendCategoryService.findAll()
                : spendCategoryService.findByType(type);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get spend category by id", description = "Returns a single spend category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found or inactive")
    })
    public ResponseEntity<SpendCategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        SpendCategoryResponseDTO category = spendCategoryService.findById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    @Operation(summary = "Create new spend category", description = "Creates a new system spend category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate category name")
    })
    public ResponseEntity<SpendCategoryResponseDTO> createCategory(@Valid @RequestBody SpendCategoryRequestDTO dto) {
        SpendCategoryResponseDTO category = spendCategoryService.create(dto);
        return new ResponseEntity<>(category, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update spend category", description = "Updates an existing spend category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate category name"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<SpendCategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody SpendCategoryRequestDTO dto) {
        SpendCategoryResponseDTO category = spendCategoryService.update(id, dto);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate spend category", description = "Soft deletes a spend category by setting active=false")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        spendCategoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

