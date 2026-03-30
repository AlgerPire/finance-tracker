package com.finance_tracker.backend_server.transaction.service;

import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AdminTransactionService {
    PagedTransactionsResponse listAllTransactions(
            Pageable pageable,
            TransactionType type,
            Long userId,
            LocalDate from,
            LocalDate to);
}
