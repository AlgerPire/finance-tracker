package com.finance_tracker.backend_server.transaction.service.impl;

import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionListResponse;
import com.finance_tracker.backend_server.transaction.entity.Transaction;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import com.finance_tracker.backend_server.transaction.mapper.TransactionMapper;
import com.finance_tracker.backend_server.transaction.repository.TransactionRepository;
import com.finance_tracker.backend_server.transaction.service.AdminTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
public class AdminTransactionServiceImpl implements AdminTransactionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("transactionAt"), Sort.Order.desc("id"));
    private static final Instant MIN_INSTANT = Instant.EPOCH;
    private static final Instant MAX_INSTANT =
            Instant.parse("9999-12-31T23:59:59.999999999Z");

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Autowired
    public AdminTransactionServiceImpl(TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedTransactionsResponse listAllTransactions(Pageable pageable, TransactionType type, Long userId, LocalDate from, LocalDate to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        Pageable effective = PageRequest.of(pageable.getPageNumber(), size, sort);

        List<TransactionType> types =
                type == null ? Arrays.asList(TransactionType.values()) : List.of(type);
        Instant fromBound =
                from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : MIN_INSTANT;
        Instant toBound =
                to != null ? to.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant() : MAX_INSTANT;

        Page<Transaction> page =
                transactionRepository.findAllForAdmin(types, userId, fromBound, toBound, effective);
        List<TransactionListResponse> content =
                page.getContent().stream()
                        .map(transactionMapper::toDto)
                        .toList();

        return new PagedTransactionsResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

}