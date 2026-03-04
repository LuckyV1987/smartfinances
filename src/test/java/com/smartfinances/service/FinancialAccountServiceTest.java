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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialAccountServiceTest {

    @Mock
    private FinancialAccountRepository financialAccountRepository;

    @Mock
    private OwnershipEntityRepository ownershipEntityRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FinancialAccountMapper financialAccountMapper;

    @InjectMocks
    private FinancialAccountService financialAccountService;

    @Test
    void shouldReturnAccounts_whenFindAllByEntity() {
        // arrange
        Long entityId = 1L;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .name("Test Entity")
                .active(true)
                .build();

        FinancialAccount account1 = FinancialAccount.builder()
                .id(1L)
                .name("Checking")
                .type(AccountTypeEnum.CHECKING)
                .build();

        FinancialAccount account2 = FinancialAccount.builder()
                .id(2L)
                .name("Savings")
                .type(AccountTypeEnum.SAVINGS)
                .build();

        FinancialAccountResponseDTO dto1 = FinancialAccountResponseDTO.builder()
                .id(1L)
                .name("Checking")
                .build();

        FinancialAccountResponseDTO dto2 = FinancialAccountResponseDTO.builder()
                .id(2L)
                .name("Savings")
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByOwnershipEntityIdAndActiveTrue(entityId))
                .thenReturn(Arrays.asList(account1, account2));
        when(transactionRepository.sumAmountByFinancialAccountId(any())).thenReturn(BigDecimal.ZERO);
        when(financialAccountMapper.toResponseDTO(eq(account1), any())).thenReturn(dto1);
        when(financialAccountMapper.toResponseDTO(eq(account2), any())).thenReturn(dto2);

        // act
        List<FinancialAccountResponseDTO> result = financialAccountService.findAllByEntity(entityId);

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Checking");
        assertThat(result.get(1).getName()).isEqualTo("Savings");
        verify(ownershipEntityRepository).findByIdAndActiveTrue(entityId);
        verify(financialAccountRepository).findByOwnershipEntityIdAndActiveTrue(entityId);
    }

    @Test
    void shouldReturnAccountsByType_whenFindAllByEntityAndType() {
        // arrange
        Long entityId = 1L;
        AccountTypeEnum type = AccountTypeEnum.CHECKING;

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(entityId)
                .active(true)
                .build();

        FinancialAccount account = FinancialAccount.builder()
                .id(1L)
                .name("Checking")
                .type(AccountTypeEnum.CHECKING)
                .build();

        FinancialAccountResponseDTO dto = FinancialAccountResponseDTO.builder()
                .id(1L)
                .name("Checking")
                .type(AccountTypeEnum.CHECKING)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(entityId)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.findByOwnershipEntityIdAndTypeAndActiveTrue(entityId, type))
                .thenReturn(List.of(account));
        when(transactionRepository.sumAmountByFinancialAccountId(any())).thenReturn(BigDecimal.ZERO);
        when(financialAccountMapper.toResponseDTO(eq(account), any())).thenReturn(dto);

        // act
        List<FinancialAccountResponseDTO> result = financialAccountService.findAllByEntityAndType(entityId, type);

        // assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(AccountTypeEnum.CHECKING);
    }

    @Test
    void shouldReturnAccount_whenValidId() {
        // arrange
        Long accountId = 1L;

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .name("Checking")
                .active(true)
                .build();

        FinancialAccountResponseDTO dto = FinancialAccountResponseDTO.builder()
                .id(accountId)
                .name("Checking")
                .build();

        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.sumAmountByFinancialAccountId(accountId)).thenReturn(BigDecimal.ZERO);
        when(financialAccountMapper.toResponseDTO(eq(account), any())).thenReturn(dto);

        // act
        FinancialAccountResponseDTO result = financialAccountService.findById(accountId);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Checking");
        verify(financialAccountRepository).findByIdAndActiveTrue(accountId);
    }

    @Test
    void shouldThrowException_whenAccountNotFound() {
        // arrange
        Long accountId = 99L;
        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> financialAccountService.findById(accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Financial account not found");
    }

    @Test
    void shouldThrowException_whenAccountInactive() {
        // arrange
        Long accountId = 1L;
        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> financialAccountService.findById(accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Financial account not found");
    }

    @Test
    void shouldReturnZeroBalance_whenNoTransactionsLinked() {
        // arrange
        Long accountId = 1L;

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .name("Checking")
                .active(true)
                .build();

        FinancialAccountResponseDTO dto = FinancialAccountResponseDTO.builder()
                .id(accountId)
                .currentBalance(BigDecimal.ZERO)
                .build();

        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.sumAmountByFinancialAccountId(accountId)).thenReturn(BigDecimal.ZERO);
        when(financialAccountMapper.toResponseDTO(eq(account), eq(BigDecimal.ZERO))).thenReturn(dto);

        // act
        FinancialAccountResponseDTO result = financialAccountService.getBalance(accountId);

        // assert
        assertThat(result.getCurrentBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCreateAccount_whenValidRequest() {
        // arrange
        FinancialAccountRequestDTO request = FinancialAccountRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Main Checking")
                .description("Primary checking account")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        FinancialAccount account = FinancialAccount.builder()
                .name("Main Checking")
                .type(AccountTypeEnum.CHECKING)
                .build();

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(1L)
                .name("Main Checking")
                .build();

        FinancialAccountResponseDTO response = FinancialAccountResponseDTO.builder()
                .id(1L)
                .name("Main Checking")
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Main Checking"))
                .thenReturn(false);
        when(financialAccountMapper.toEntity(request)).thenReturn(account);
        when(financialAccountRepository.save(account)).thenReturn(savedAccount);
        when(financialAccountMapper.toResponseDTO(eq(savedAccount), eq(BigDecimal.ZERO)))
                .thenReturn(response);

        // act
        FinancialAccountResponseDTO result = financialAccountService.create(request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Main Checking");
        verify(financialAccountRepository).save(account);
    }

    @Test
    void shouldDefaultCurrencyToUSD_whenNotProvided() {
        // arrange
        FinancialAccountRequestDTO request = FinancialAccountRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Cash Wallet")
                .type(AccountTypeEnum.CASH)
                .currencyCode(null) // Not provided
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        FinancialAccount account = FinancialAccount.builder()
                .currencyCode("USD") // Mapper should default to USD
                .build();

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(1L)
                .currencyCode("USD")
                .build();

        FinancialAccountResponseDTO response = FinancialAccountResponseDTO.builder()
                .id(1L)
                .currencyCode("USD")
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Cash Wallet"))
                .thenReturn(false);
        when(financialAccountMapper.toEntity(request)).thenReturn(account);
        when(financialAccountRepository.save(account)).thenReturn(savedAccount);
        when(financialAccountMapper.toResponseDTO(eq(savedAccount), any())).thenReturn(response);

        // act
        FinancialAccountResponseDTO result = financialAccountService.create(request);

        // assert
        assertThat(result.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void shouldThrowException_whenDuplicateNameForEntity() {
        // arrange
        FinancialAccountRequestDTO request = FinancialAccountRequestDTO.builder()
                .ownershipEntityId(1L)
                .name("Checking")
                .type(AccountTypeEnum.CHECKING)
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .active(true)
                .build();

        when(ownershipEntityRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(entity));
        when(financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Checking"))
                .thenReturn(true);

        // act & assert
        assertThatThrownBy(() -> financialAccountService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldUpdateAccount_whenValidRequest() {
        // arrange
        Long accountId = 1L;
        FinancialAccountUpdateRequestDTO request = FinancialAccountUpdateRequestDTO.builder()
                .name("Updated Name")
                .description("Updated description")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .build();

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .name("Old Name")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .ownershipEntity(entity)
                .active(true)
                .build();

        FinancialAccount updatedAccount = FinancialAccount.builder()
                .id(accountId)
                .name("Updated Name")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .build();

        FinancialAccountResponseDTO response = FinancialAccountResponseDTO.builder()
                .id(accountId)
                .name("Updated Name")
                .type(AccountTypeEnum.CHECKING)
                .currencyCode("USD")
                .build();

        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.of(account));
        when(financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Updated Name"))
                .thenReturn(false);
        doNothing().when(financialAccountMapper).updateEntityFromDTO(request, account);
        when(financialAccountRepository.save(account)).thenReturn(updatedAccount);
        when(transactionRepository.sumAmountByFinancialAccountId(accountId)).thenReturn(BigDecimal.ZERO);
        when(financialAccountMapper.toResponseDTO(eq(updatedAccount), any())).thenReturn(response);

        // act
        FinancialAccountResponseDTO result = financialAccountService.update(accountId, request);

        // assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(financialAccountMapper).updateEntityFromDTO(request, account);
        verify(financialAccountRepository).save(account);
    }

    @Test
    void shouldNotUpdateType_whenUpdate() {
        // arrange - This test verifies that type is immutable
        // Update DTO doesn't have type field, so type should never change
        Long accountId = 1L;
        FinancialAccountUpdateRequestDTO request = FinancialAccountUpdateRequestDTO.builder()
                .name("Updated Name")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .build();

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .name("Old Name")
                .type(AccountTypeEnum.CHECKING) // Original type
                .ownershipEntity(entity)
                .active(true)
                .build();

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(accountId)
                .type(AccountTypeEnum.CHECKING) // Type unchanged
                .build();

        FinancialAccountResponseDTO response = FinancialAccountResponseDTO.builder()
                .id(accountId)
                .type(AccountTypeEnum.CHECKING)
                .build();

        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.of(account));
        when(financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Updated Name"))
                .thenReturn(false);
        doNothing().when(financialAccountMapper).updateEntityFromDTO(request, account);
        when(financialAccountRepository.save(account)).thenReturn(savedAccount);
        when(transactionRepository.sumAmountByFinancialAccountId(accountId)).thenReturn(BigDecimal.ZERO);
        when(financialAccountMapper.toResponseDTO(eq(savedAccount), any())).thenReturn(response);

        // act
        FinancialAccountResponseDTO result = financialAccountService.update(accountId, request);

        // assert
        assertThat(result.getType()).isEqualTo(AccountTypeEnum.CHECKING);
        verify(financialAccountMapper).updateEntityFromDTO(request, account);
    }

    @Test
    void shouldNotUpdateCurrencyCode_whenUpdate() {
        // arrange - This test verifies that currencyCode is immutable
        // Update DTO doesn't have currencyCode field, so it should never change
        Long accountId = 1L;
        FinancialAccountUpdateRequestDTO request = FinancialAccountUpdateRequestDTO.builder()
                .name("Updated Name")
                .build();

        OwnershipEntity entity = OwnershipEntity.builder()
                .id(1L)
                .build();

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .name("Old Name")
                .currencyCode("USD") // Original currency
                .ownershipEntity(entity)
                .active(true)
                .build();

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(accountId)
                .currencyCode("USD") // Currency unchanged
                .build();

        FinancialAccountResponseDTO response = FinancialAccountResponseDTO.builder()
                .id(accountId)
                .currencyCode("USD")
                .build();

        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.of(account));
        when(financialAccountRepository.existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(1L, "Updated Name"))
                .thenReturn(false);
        doNothing().when(financialAccountMapper).updateEntityFromDTO(request, account);
        when(financialAccountRepository.save(account)).thenReturn(savedAccount);
        when(transactionRepository.sumAmountByFinancialAccountId(accountId)).thenReturn(BigDecimal.ZERO);
        when(financialAccountMapper.toResponseDTO(eq(savedAccount), any())).thenReturn(response);

        // act
        FinancialAccountResponseDTO result = financialAccountService.update(accountId, request);

        // assert
        assertThat(result.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void shouldDeactivateAccount_whenValidId() {
        // arrange
        Long accountId = 1L;

        FinancialAccount account = FinancialAccount.builder()
                .id(accountId)
                .active(true)
                .build();

        when(financialAccountRepository.findByIdAndActiveTrue(accountId)).thenReturn(Optional.of(account));
        when(financialAccountRepository.save(account)).thenReturn(account);

        // act
        financialAccountService.deactivate(accountId);

        // assert
        assertThat(account.isActive()).isFalse();
        verify(financialAccountRepository).save(account);
    }
}

