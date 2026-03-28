package com.finance_tracker.backend_server.transaction.service.impl;

import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.common.exception.AccountNotFoundException;
import com.finance_tracker.backend_server.common.exception.InsufficientFundsException;
import com.finance_tracker.backend_server.common.exception.InvalidTransactionException;
import com.finance_tracker.backend_server.common.util.SecurityContextService;
import com.finance_tracker.backend_server.transaction.dto.CreateTransactionRequest;
import com.finance_tracker.backend_server.transaction.dto.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.TransactionListResponse;
import com.finance_tracker.backend_server.transaction.dto.TransactionResponse;
import com.finance_tracker.backend_server.transaction.entity.Transaction;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
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
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final SecurityContextService securityContextService;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("transactionAt"), Sort.Order.desc("id"));

    @Autowired
    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            SecurityContextService securityContextService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.securityContextService = securityContextService;
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
    public PagedTransactionsResponse listTransactionsForCurrentUser(Pageable pageable, TransactionType type) {
        User user = securityContextService.getCurrentUser();
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        Pageable effective = PageRequest.of(pageable.getPageNumber(), size, sort);

        Page<Transaction> page =
                transactionRepository.findAllInvolvingAccountsOfUser(user.getId(), type, effective);
        List<TransactionListResponse> content =
                page.getContent().stream().map(TransactionServiceImpl::toListResponse).toList();

        return new PagedTransactionsResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
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

        return toResponse(depositTransaction, account.getBalance());
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

        return toResponse(withdrawalTransaction, currentAccount.getBalance());
    }

    private TransactionResponse transfer(
            User user,
            BigDecimal amount,
            Long sourceAccountId,
            Long targetAccountId,
            String description,
            Instant when) {
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

        return toResponse(transferTransaction, sourceAccount.getBalance());
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

    private static TransactionResponse toResponse(
            Transaction transaction,
            BigDecimal accountBalanceAfter) {
        Account currentAccount = transaction.getAccount();
        Account sourceAccount = transaction.getSourceAccount();
        Account targetAccount = transaction.getTargetAccount();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                currentAccount != null ? currentAccount.getId() : null,
                sourceAccount != null ? sourceAccount.getId() : null,
                targetAccount != null ? targetAccount.getId() : null,
                accountBalanceAfter,
                transaction.getDescription(),
                transaction.getTransactionAt(),
                transaction.getCreatedAt());
    }

    private static TransactionListResponse toListResponse(Transaction transaction) {
        Account currentAccount = transaction.getAccount();
        Account sourceAccount = transaction.getSourceAccount();
        Account targetAccount = transaction.getTargetAccount();
        return new TransactionListResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                currentAccount != null ? currentAccount.getCurrencyType().toString() : null,
                currentAccount != null ? currentAccount.getId() : null,
                sourceAccount != null ? sourceAccount.getId() : null,
                targetAccount != null ? targetAccount.getId() : null,
                sourceAccount != null ? sourceAccount.getUser().getUsername() : null,
                sourceAccount != null ? sourceAccount.getAccountIdentification() : null,
                targetAccount != null ? targetAccount.getUser().getUsername() : null,
                targetAccount != null ? targetAccount.getAccountIdentification() : null,
                transaction.getDescription(),
                transaction.getTransactionAt(),
                transaction.getCreatedAt());
    }
}