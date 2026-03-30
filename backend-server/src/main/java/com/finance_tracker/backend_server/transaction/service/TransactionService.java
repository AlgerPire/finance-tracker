package com.finance_tracker.backend_server.transaction.service;

import com.finance_tracker.backend_server.transaction.dto.request.CreateTransactionRequest;
import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionResponse;
import com.finance_tracker.backend_server.transaction.dto.request.TransferRequest;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TransactionService {
    TransactionResponse createTransaction(CreateTransactionRequest request);

    PagedTransactionsResponse listTransactionsForCurrentUser(
            Pageable pageable, TransactionType type, LocalDate from, LocalDate to);

    TransactionResponse transferUsingAccountIdentification(TransferRequest request);


}
