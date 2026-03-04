package com.smartfinances.controller;

import com.smartfinances.dto.request.TransactionDetailRequestDTO;
import com.smartfinances.dto.request.TransactionRequestDTO;
import com.smartfinances.dto.response.TransactionDetailResponseDTO;
import com.smartfinances.dto.response.TransactionResponseDTO;
import com.smartfinances.service.TransactionDetailService;
import com.smartfinances.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction management operations")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionDetailService transactionDetailService;

    public TransactionController(
            TransactionService transactionService,
            TransactionDetailService transactionDetailService) {
        this.transactionService = transactionService;
        this.transactionDetailService = transactionDetailService;
    }

    @GetMapping
    @Operation(summary = "Get transactions", description = "Returns transactions filtered by ownershipEntityId, financialAccountId, or budgetAllocationId")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactions(
            @RequestParam(required = false) Long ownershipEntityId,
            @RequestParam(required = false) Long financialAccountId,
            @RequestParam(required = false) Long budgetAllocationId) {

        List<TransactionResponseDTO> transactions;

        if (ownershipEntityId != null) {
            transactions = transactionService.findAllByEntity(ownershipEntityId);
        } else if (financialAccountId != null) {
            transactions = transactionService.findAllByAccount(financialAccountId);
        } else if (budgetAllocationId != null) {
            transactions = transactionService.findAllByAllocation(budgetAllocationId);
        } else {
            throw new IllegalArgumentException("At least one query parameter is required: ownershipEntityId, financialAccountId, or budgetAllocationId");
        }

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Returns a single transaction by ID")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(@PathVariable Long id) {
        TransactionResponseDTO transaction = transactionService.findById(id);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping
    @Operation(summary = "Create transaction", description = "Creates a new transaction (CARRYOVER type rejected)")
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody TransactionRequestDTO dto) {
        TransactionResponseDTO transaction = transactionService.create(dto);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Get transaction details", description = "Returns all line items for a transaction (lazy loaded)")
    public ResponseEntity<List<TransactionDetailResponseDTO>> getTransactionDetails(@PathVariable Long id) {
        List<TransactionDetailResponseDTO> details = transactionDetailService.findByTransaction(id);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{id}/details")
    @Operation(summary = "Add transaction detail", description = "Adds a line item to a transaction")
    public ResponseEntity<List<TransactionDetailResponseDTO>> addTransactionDetail(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDetailRequestDTO dto) {
        List<TransactionDetailResponseDTO> details = transactionDetailService.addDetail(id, dto);
        return ResponseEntity.ok(details);
    }

    @DeleteMapping("/{id}/details/{detailId}")
    @Operation(summary = "Remove transaction detail", description = "Removes a line item from a transaction")
    public ResponseEntity<List<TransactionDetailResponseDTO>> removeTransactionDetail(
            @PathVariable Long id,
            @PathVariable Long detailId) {
        List<TransactionDetailResponseDTO> details = transactionDetailService.removeDetail(id, detailId);
        return ResponseEntity.ok(details);
    }

    // NO PUT endpoint — transactions are immutable
    // NO DELETE endpoint — use ADJUSTMENT transaction to correct
}

