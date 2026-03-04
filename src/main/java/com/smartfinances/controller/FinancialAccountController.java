package com.smartfinances.controller;

import com.smartfinances.dto.request.FinancialAccountRequestDTO;
import com.smartfinances.dto.request.FinancialAccountUpdateRequestDTO;
import com.smartfinances.dto.response.FinancialAccountResponseDTO;
import com.smartfinances.entity.enums.AccountTypeEnum;
import com.smartfinances.service.FinancialAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financial-accounts")
@Tag(name = "Financial Accounts", description = "Financial account management operations")
public class FinancialAccountController {

    private final FinancialAccountService financialAccountService;

    public FinancialAccountController(FinancialAccountService financialAccountService) {
        this.financialAccountService = financialAccountService;
    }

    @GetMapping
    @Operation(summary = "Get all financial accounts", description = "Returns all active financial accounts for an ownership entity, optionally filtered by type")
    public ResponseEntity<List<FinancialAccountResponseDTO>> getAllAccounts(
            @RequestParam Long ownershipEntityId,
            @RequestParam(required = false) AccountTypeEnum type) {
        List<FinancialAccountResponseDTO> accounts;
        if (type != null) {
            accounts = financialAccountService.findAllByEntityAndType(ownershipEntityId, type);
        } else {
            accounts = financialAccountService.findAllByEntity(ownershipEntityId);
        }
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get financial account by ID", description = "Returns a single financial account by ID")
    public ResponseEntity<FinancialAccountResponseDTO> getAccountById(@PathVariable Long id) {
        FinancialAccountResponseDTO account = financialAccountService.findById(id);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Check account balance", description = "Forces fresh balance calculation for an account")
    public ResponseEntity<FinancialAccountResponseDTO> getBalance(@PathVariable Long id) {
        FinancialAccountResponseDTO account = financialAccountService.getBalance(id);
        return ResponseEntity.ok(account);
    }

    @PostMapping
    @Operation(summary = "Create financial account", description = "Creates a new financial account")
    public ResponseEntity<FinancialAccountResponseDTO> createAccount(
            @Valid @RequestBody FinancialAccountRequestDTO dto) {
        FinancialAccountResponseDTO account = financialAccountService.create(dto);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update financial account", description = "Updates an existing financial account")
    public ResponseEntity<FinancialAccountResponseDTO> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody FinancialAccountUpdateRequestDTO dto) {
        FinancialAccountResponseDTO account = financialAccountService.update(id, dto);
        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate financial account", description = "Soft deletes a financial account by setting active to false")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long id) {
        financialAccountService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

