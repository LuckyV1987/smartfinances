package com.smartfinances.service;

import com.smartfinances.dto.request.TransactionDetailRequestDTO;
import com.smartfinances.dto.response.TransactionDetailResponseDTO;
import com.smartfinances.entity.Transaction;
import com.smartfinances.entity.TransactionDetail;
import com.smartfinances.exception.InvalidRequestException;
import com.smartfinances.exception.ResourceNotFoundException;
import com.smartfinances.mapper.TransactionMapper;
import com.smartfinances.repository.TransactionDetailRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionDetailServiceTest {

    @Mock
    private TransactionDetailRepository transactionDetailRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionDetailService transactionDetailService;

    @Test
    void shouldReturnDetails_whenFindByTransaction() {
        // arrange
        Long transactionId = 1L;

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .build();

        TransactionDetail detail1 = TransactionDetail.builder()
                .id(1L)
                .name("Item 1")
                .build();

        TransactionDetail detail2 = TransactionDetail.builder()
                .id(2L)
                .name("Item 2")
                .build();

        TransactionDetailResponseDTO dto1 = TransactionDetailResponseDTO.builder()
                .id(1L)
                .name("Item 1")
                .build();

        TransactionDetailResponseDTO dto2 = TransactionDetailResponseDTO.builder()
                .id(2L)
                .name("Item 2")
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionDetailRepository.findByTransactionId(transactionId))
                .thenReturn(Arrays.asList(detail1, detail2));
        when(transactionMapper.toDetailResponseDTO(detail1)).thenReturn(dto1);
        when(transactionMapper.toDetailResponseDTO(detail2)).thenReturn(dto2);

        // act
        List<TransactionDetailResponseDTO> result = transactionDetailService.findByTransaction(transactionId);

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Item 1");
        assertThat(result.get(1).getName()).isEqualTo("Item 2");
    }

    @Test
    void shouldThrowException_whenTransactionNotFoundOnFindDetails() {
        // arrange
        Long transactionId = 99L;
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> transactionDetailService.findByTransaction(transactionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void shouldAddDetail_whenValidRequest() {
        // arrange
        Long transactionId = 1L;

        TransactionDetailRequestDTO request = TransactionDetailRequestDTO.builder()
                .name("Milk")
                .quantity(new BigDecimal("2.000"))
                .unit("litre")
                .unitPrice(new BigDecimal("3.50"))
                .totalPrice(new BigDecimal("7.00")) // 2 * 3.50 = 7.00
                .build();

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .build();

        TransactionDetail detail = TransactionDetail.builder()
                .transaction(transaction)
                .name("Milk")
                .build();

        TransactionDetail savedDetail = TransactionDetail.builder()
                .id(1L)
                .name("Milk")
                .build();

        TransactionDetailResponseDTO responseDto = TransactionDetailResponseDTO.builder()
                .id(1L)
                .name("Milk")
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toDetailEntity(request, transaction)).thenReturn(detail);
        when(transactionDetailRepository.save(detail)).thenReturn(savedDetail);
        when(transactionDetailRepository.findByTransactionId(transactionId)).thenReturn(List.of(savedDetail));
        when(transactionMapper.toDetailResponseDTO(savedDetail)).thenReturn(responseDto);

        // act
        List<TransactionDetailResponseDTO> result = transactionDetailService.addDetail(transactionId, request);

        // assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Milk");
        verify(transactionDetailRepository).save(detail);
    }

    @Test
    void shouldThrowException_whenTotalPriceMismatch() {
        // arrange
        Long transactionId = 1L;

        TransactionDetailRequestDTO request = TransactionDetailRequestDTO.builder()
                .name("Bread")
                .quantity(new BigDecimal("3.000"))
                .unitPrice(new BigDecimal("2.50"))
                .totalPrice(new BigDecimal("10.00")) // Wrong! Should be 7.50
                .build();

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // act & assert
        assertThatThrownBy(() -> transactionDetailService.addDetail(transactionId, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Total price mismatch");
    }

    @Test
    void shouldRemoveDetail_whenValidRequest() {
        // arrange
        Long transactionId = 1L;
        Long detailId = 1L;

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .build();

        TransactionDetail detail = TransactionDetail.builder()
                .id(detailId)
                .transaction(transaction)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionDetailRepository.findById(detailId)).thenReturn(Optional.of(detail));
        doNothing().when(transactionDetailRepository).delete(detail);
        when(transactionDetailRepository.findByTransactionId(transactionId)).thenReturn(List.of());

        // act
        List<TransactionDetailResponseDTO> result = transactionDetailService.removeDetail(transactionId, detailId);

        // assert
        assertThat(result).isEmpty();
        verify(transactionDetailRepository).delete(detail);
    }

    @Test
    void shouldThrowException_whenDetailNotFound() {
        // arrange
        Long transactionId = 1L;
        Long detailId = 99L;

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionDetailRepository.findById(detailId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> transactionDetailService.removeDetail(transactionId, detailId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction detail not found");
    }
}

