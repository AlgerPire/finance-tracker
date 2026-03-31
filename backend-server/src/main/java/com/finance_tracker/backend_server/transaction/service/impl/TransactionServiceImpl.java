package com.finance_tracker.backend_server.transaction.service.impl;

import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.common.exception.AccountNotFoundException;
import com.finance_tracker.backend_server.common.exception.InsufficientFundsException;
import com.finance_tracker.backend_server.common.exception.InvalidTransactionException;
import com.finance_tracker.backend_server.common.util.SecurityContextService;
import com.finance_tracker.backend_server.transaction.dto.request.CreateTransactionRequest;
import com.finance_tracker.backend_server.transaction.dto.request.TransferRequest;
import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionListResponse;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionResponse;
import com.finance_tracker.backend_server.transaction.entity.Transaction;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import com.finance_tracker.backend_server.transaction.mapper.TransactionMapper;
import com.finance_tracker.backend_server.transaction.repository.TransactionRepository;
import com.finance_tracker.backend_server.transaction.service.TransactionService;
import com.finance_tracker.backend_server.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Implementation of the {@link TransactionService} interface.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    /**
     * The repository for managing transactions.
     */
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final SecurityContextService securityContextService;
    private final TransactionMapper transactionMapper;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("transactionAt"), Sort.Order.desc("id"));

    @Autowired
    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            SecurityContextService securityContextService, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.securityContextService = securityContextService;
        this.transactionMapper = transactionMapper;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        User user = securityContextService.getCurrentUser();
        BigDecimal amount = request.amount();
        Instant when = request.transactionAt() != null ? request.transactionAt() : Instant.now();

        return switch (request.type()) {
            case DEPOSIT -> deposit(user, amount, request.accountId(), request.description(), when);
            case WITHDRAWAL -> withdrawal(user, amount, request.accountId(), request.description(), when);
            case TRANSFER -> transfer(
                    user, amount, request.sourceAccountId(), request.targetAccountId(), request.description(), when);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PagedTransactionsResponse listTransactionsForCurrentUser(Pageable pageable, TransactionType type, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidTransactionException("from must be before or equal to to");
        }
        Instant fromInstant =
                from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant =
                to != null ? to.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant() : null;
        User user = securityContextService.getCurrentUser();
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        Pageable effective = PageRequest.of(pageable.getPageNumber(), size, sort);

        Page<Transaction> page =
                transactionRepository.findAllInvolvingAccountsOfUser(user.getId(), type, fromInstant, toInstant, effective);
        List<TransactionListResponse> content =
                page.getContent().stream().map(transactionMapper::toDto).toList();

        return new PagedTransactionsResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedTransactionsResponse listTransactionsByAccountId(Long accountId, Pageable pageable) {
        User owner = securityContextService.getCurrentUser();
        accountRepository.findByIdAndUser_Id(accountId, owner.getId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found or does not belong to the authenticated user"));

        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        Pageable effective = PageRequest.of(pageable.getPageNumber(), size, sort);

        Page<Transaction> page = transactionRepository.findAllByAccountId(accountId, effective);
        List<TransactionListResponse> content =
                page.getContent().stream().map(transactionMapper::toDto).toList();

        return new PagedTransactionsResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Override
    @Transactional
    public TransactionResponse transferUsingAccountIdentification(TransferRequest request) {
        if (request.sourceAccountIdentification()
                .equalsIgnoreCase(request.targetAccountIdentification())) {
            throw new InvalidTransactionException("Source and target account must differ");
        }
        User user = securityContextService.getCurrentUser();
        Instant when = request.transactionAt() != null ? request.transactionAt() : Instant.now();

        Account source = accountRepository
                .findByAccountIdentificationAndUser_IdAndActiveTrue(
                        request.sourceAccountIdentification(), user.getId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Source account not found, inactive, or not owned by you"));

        Account target = accountRepository
                .findByAccountIdentificationAndActiveTrue(
                        request.targetAccountIdentification())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Target account not found or inactive"));

        if (source.getCurrencyType() != target.getCurrencyType()) {
            throw new InvalidTransactionException(
                    "Transfer is only allowed between accounts with the same currency");
        }
        assertSufficientBalance(source, request.amount());
        source.setBalance(source.getBalance().subtract(request.amount()));
        target.setBalance(target.getBalance().add(request.amount()));
        accountRepository.save(source);
        accountRepository.save(target);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setAmount(request.amount());
        transaction.setAccount(source);
        transaction.setSourceAccount(source);
        transaction.setTargetAccount(target);
        transaction.setDescription(request.description());
        transaction.setTransactionAt(when);
        transaction = transactionRepository.save(transaction);

        return transactionMapper.toTransactionResponse(transaction, source.getBalance());
    }

    private TransactionResponse deposit(User user, BigDecimal amount, Long accountId, String description, Instant when) {
        if (accountId == null) {
            throw new InvalidTransactionException("Account Id is required for a deposit!");
        }
        Account account = loadActiveOwnedAccount(accountId, user.getId());
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction depositTransaction = new Transaction();
        depositTransaction.setUser(user);
        depositTransaction.setType(TransactionType.DEPOSIT);
        depositTransaction.setAmount(amount);
        depositTransaction.setAccount(account);
        depositTransaction.setDescription(description);
        depositTransaction.setTransactionAt(when);
        depositTransaction = transactionRepository.save(depositTransaction);

        return transactionMapper.toTransactionResponse(depositTransaction, account.getBalance());
    }

    private TransactionResponse withdrawal(User user, BigDecimal amount, Long accountId, String description, Instant when) {
        if (accountId == null) {
            throw new InvalidTransactionException("accountId is required for a withdrawal");
        }
        Account currentAccount = loadActiveOwnedAccount(accountId, user.getId());
        assertSufficientBalance(currentAccount, amount);
        currentAccount.setBalance(currentAccount.getBalance().subtract(amount));
        accountRepository.save(currentAccount);

        Transaction withdrawalTransaction = new Transaction();
        withdrawalTransaction.setUser(user);
        withdrawalTransaction.setType(TransactionType.WITHDRAWAL);
        withdrawalTransaction.setAmount(amount);
        withdrawalTransaction.setAccount(currentAccount);
        withdrawalTransaction.setDescription(description);
        withdrawalTransaction.setTransactionAt(when);
        withdrawalTransaction = transactionRepository.save(withdrawalTransaction);

        return transactionMapper.toTransactionResponse(withdrawalTransaction, currentAccount.getBalance());
    }

    private TransactionResponse transfer(User user, BigDecimal amount, Long sourceAccountId, Long targetAccountId, String description, Instant when) {
        if (sourceAccountId == null || targetAccountId == null) {
            throw new InvalidTransactionException("sourceAccountId and targetAccountId are required for a transfer");
        }
        if (sourceAccountId.equals(targetAccountId)) {
            throw new InvalidTransactionException("Source and target account must differ");
        }
        Account sourceAccount = loadActiveOwnedAccount(sourceAccountId, user.getId());
        Account targetAccount = loadTargetAccountDetails(targetAccountId);
        if (sourceAccount.getCurrencyType() != targetAccount.getCurrencyType()) {
            throw new InvalidTransactionException("Transfer is only allowed between accounts with the same currency");
        }
        assertSufficientBalance(sourceAccount, amount);
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        Transaction transferTransaction = new Transaction();
        transferTransaction.setUser(user);
        transferTransaction.setType(TransactionType.TRANSFER);
        transferTransaction.setAmount(amount);
        transferTransaction.setAccount(sourceAccount);
        transferTransaction.setSourceAccount(sourceAccount);
        transferTransaction.setTargetAccount(targetAccount);
        transferTransaction.setDescription(description);
        transferTransaction.setTransactionAt(when);
        transferTransaction = transactionRepository.save(transferTransaction);

        return transactionMapper.toTransactionResponse(transferTransaction, sourceAccount.getBalance());
    }

    private Account loadActiveOwnedAccount(Long accountId, Long userId) {
        return accountRepository
                .findByIdAndUser_IdAndActiveTrue(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found!"));
    }

    private Account loadTargetAccountDetails(Long accountId) {
        return accountRepository
                .findByIdAndActiveTrue(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found!"));
    }

    private static void assertSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for this transaction");
        }
    }

}