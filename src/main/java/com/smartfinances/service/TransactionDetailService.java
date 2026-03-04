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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionDetailService {

    private final TransactionDetailRepository transactionDetailRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public TransactionDetailService(
            TransactionDetailRepository transactionDetailRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper) {
        this.transactionDetailRepository = transactionDetailRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public List<TransactionDetailResponseDTO> findByTransaction(Long transactionId) {
        // Verify transaction exists
        transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        List<TransactionDetail> details = transactionDetailRepository.findByTransactionId(transactionId);
        return details.stream()
                .map(transactionMapper::toDetailResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TransactionDetailResponseDTO> addDetail(Long transactionId, TransactionDetailRequestDTO dto) {
        // Find transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Validate totalPrice == quantity * unitPrice within 0.01 tolerance
        BigDecimal calculatedTotal = dto.getQuantity().multiply(dto.getUnitPrice()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal difference = dto.getTotalPrice().subtract(calculatedTotal).abs();

        if (difference.compareTo(new BigDecimal("0.01")) >= 0) {
            throw new InvalidRequestException(
                    String.format("Total price mismatch: expected %.4f (quantity * unitPrice), got %.4f",
                            calculatedTotal, dto.getTotalPrice()));
        }

        // Create detail
        TransactionDetail detail = transactionMapper.toDetailEntity(dto, transaction);
        transactionDetailRepository.save(detail);

        // Return updated full detail list
        return findByTransaction(transactionId);
    }

    @Transactional
    public List<TransactionDetailResponseDTO> removeDetail(Long transactionId, Long detailId) {
        // Find transaction
        transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));

        // Find detail
        TransactionDetail detail = transactionDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction detail not found with id: " + detailId));

        // Verify detail belongs to this transaction
        if (!detail.getTransaction().getId().equals(transactionId)) {
            throw new ResourceNotFoundException("Detail " + detailId + " does not belong to transaction " + transactionId);
        }

        // Hard delete (details have no audit requirement independently)
        transactionDetailRepository.delete(detail);

        // Return updated full detail list
        return findByTransaction(transactionId);
    }
}

