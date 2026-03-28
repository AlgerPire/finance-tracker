package com.finance_tracker.backend_server.transaction.service;

import com.finance_tracker.backend_server.transaction.dto.CreateTransactionRequest;
import com.finance_tracker.backend_server.transaction.dto.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.TransactionResponse;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    TransactionResponse createTransaction(CreateTransactionRequest request);

    PagedTransactionsResponse listTransactionsForCurrentUser(Pageable pageable, TransactionType type);
}
