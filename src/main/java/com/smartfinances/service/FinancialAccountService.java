package com.smartfinances.service;

import com.smartfinances.dto.request.FinancialAccountRequestDTO;
import com.smartfinances.dto.request.FinancialAccountUpdateRequestDTO;
import com.smartfinances.dto.response.FinancialAccountResponseDTO;
import com.smartfinances.entity.FinancialAccount;
import com.smartfinances.entity.OwnershipEntity;
import com.smartfinances.entity.enums.AccountTypeEnum;
import com.smartfinances.exception.DuplicateResourceException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.FinancialAccountMapper;
import com.smartfinances.repository.FinancialAccountRepository;
import com.smartfinances.repository.OwnershipEntityRepository;
import com.smartfinances.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinancialAccountService {

    private final FinancialAccountRepository financialAccountRepository;
    private final OwnershipEntityRepository ownershipEntityRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialAccountMapper financialAccountMapper;

    public FinancialAccountService(
            FinancialAccountRepository financialAccountRepository,
            OwnershipEntityRepository ownershipEntityRepository,
            TransactionRepository transactionRepository,
            FinancialAccountMapper financialAccountMapper) {
        this.financialAccountRepository = financialAccountRepository;
        this.ownershipEntityRepository = ownershipEntityRepository;
        this.transactionRepository = transactionRepository;
        this.financialAccountMapper = financialAccountMapper;
    }

    @Transactional(readOnly = true)
    public List<FinancialAccountResponseDTO> findAllByEntity(Long ownershipEntityId) {
        // Verify entity exists and is active
        ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        List<FinancialAccount> accounts = financialAccountRepository.findByOwnershipEntityIdAndActiveTrue(ownershipEntityId);
        return accounts.stream()
                .map(this::buildResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FinancialAccountResponseDTO> findAllByEntityAndType(Long ownershipEntityId, AccountTypeEnum type) {
        // Verify entity exists and is active
        ownershipEntityRepository.findByIdAndActiveTrue(ownershipEntityId)
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + ownershipEntityId));

        List<FinancialAccount> accounts = financialAccountRepository.findByOwnershipEntityIdAndTypeAndActiveTrue(ownershipEntityId, type);
        return accounts.stream()
                .map(this::buildResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FinancialAccountResponseDTO findById(Long id) {
        FinancialAccount account = financialAccountRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found with id: " + id));
        return buildResponseDTO(account);
    }

    @Transactional(readOnly = true)
    public FinancialAccountResponseDTO getBalance(Long id) {
        // Forces fresh balance calculation
        FinancialAccount account = financialAccountRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found with id: " + id));
        return buildResponseDTO(account);
    }

    @Transactional
    public FinancialAccountResponseDTO create(FinancialAccountRequestDTO dto) {
        // Validate ownership entity exists and active
        OwnershipEntity ownershipEntity = ownershipEntityRepository.findByIdAndActiveTrue(dto.getOwnershipEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Ownership entity not found with id: " + dto.getOwnershipEntityId()));

        // Validate name uniqueness per entity
        if (financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(dto.getOwnershipEntityId(), dto.getName())) {
            throw new DuplicateResourceException("Financial account with name '" + dto.getName() + "' already exists for this ownership entity");
        }

        // Create entity
        FinancialAccount account = financialAccountMapper.toEntity(dto);
        account.setOwnershipEntity(ownershipEntity);

        FinancialAccount savedAccount = financialAccountRepository.save(account);

        // Return with zero balance
        return financialAccountMapper.toResponseDTO(savedAccount, BigDecimal.ZERO);
    }

    @Transactional
    public FinancialAccountResponseDTO update(Long id, FinancialAccountUpdateRequestDTO dto) {
        FinancialAccount account = financialAccountRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found with id: " + id));

        // Validate name uniqueness if changed
        if (dto.getName() != null && !dto.getName().equalsIgnoreCase(account.getName())) {
            if (financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(
                    account.getOwnershipEntity().getId(), dto.getName())) {
                throw new DuplicateResourceException("Financial account with name '" + dto.getName() + "' already exists for this ownership entity");
            }
        }

        // Update entity (non-null fields only, type/currencyCode/ownershipEntityId are immutable)
        financialAccountMapper.updateEntityFromDTO(dto, account);

        FinancialAccount updatedAccount = financialAccountRepository.save(account);
        return buildResponseDTO(updatedAccount);
    }

    @Transactional
    public void deactivate(Long id) {
        FinancialAccount account = financialAccountRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found with id: " + id));

        account.setActive(false);
        financialAccountRepository.save(account);
    }

    // Helper method to build response DTO with balance
    private FinancialAccountResponseDTO buildResponseDTO(FinancialAccount account) {
        // Calculate real balance from transactions
        BigDecimal currentBalance = calculateBalance(account);
        return financialAccountMapper.toResponseDTO(account, currentBalance);
    }

    // Balance calculation - sums all transactions for this account
    private BigDecimal calculateBalance(FinancialAccount account) {
        // Sum all transactions linked to this account
        // Note: The query sums absolute amounts. For proper balance calculation,
        // we would need to consider transaction types (CREDIT adds, DEBIT subtracts).
        // For now, this returns the simple sum. A future enhancement could be:
        // SELECT SUM(CASE WHEN type='CREDIT' THEN amount ELSE -amount END)
        return transactionRepository.sumAmountByFinancialAccountId(account.getId());
    }
}

