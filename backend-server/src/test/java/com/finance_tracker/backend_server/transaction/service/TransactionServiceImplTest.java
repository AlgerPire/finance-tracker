package com.finance_tracker.backend_server.transaction.service;

import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.common.exception.AccountNotFoundException;
import com.finance_tracker.backend_server.common.exception.InsufficientFundsException;
import com.finance_tracker.backend_server.common.exception.InvalidTransactionException;
import com.finance_tracker.backend_server.common.util.SecurityContextService;
import com.finance_tracker.backend_server.transaction.dto.request.CreateTransactionRequest;
import com.finance_tracker.backend_server.transaction.dto.request.TransferRequest;
import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionResponse;
import com.finance_tracker.backend_server.transaction.entity.Transaction;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import com.finance_tracker.backend_server.transaction.mapper.TransactionMapper;
import com.finance_tracker.backend_server.transaction.repository.TransactionRepository;
import com.finance_tracker.backend_server.transaction.service.impl.TransactionServiceImpl;
import com.finance_tracker.backend_server.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl")
class TransactionServiceImplTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountRepository accountRepository;
    @Mock SecurityContextService securityContextService;
    @Mock TransactionMapper transactionMapper;

    @InjectMocks TransactionServiceImpl service;

    private User user;
    private Account account;
    private Transaction savedTx;
    private TransactionResponse txResponse;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "pwd");
        user.setId(1L);

        account = new Account();
        account.setId(10L);
        account.setUser(user);
        account.setAccountType(AccountType.SAVINGS);
        account.setCurrencyType(CurrencyType.EUR);
        account.setBalance(BigDecimal.valueOf(1000));
        account.setAccountIdentification("K042M3");
        account.setActive(true);

        savedTx = new Transaction();
        savedTx.setId(100L);
        savedTx.setUser(user);
        savedTx.setAmount(BigDecimal.valueOf(200));
        savedTx.setTransactionAt(Instant.now());

        txResponse = new TransactionResponse(
                100L, TransactionType.DEPOSIT, BigDecimal.valueOf(200),
                10L, null, null, BigDecimal.valueOf(1200),
                "desc", Instant.now(), Instant.now());
    }

    // -----------------------------------------------------------------------
    // DEPOSIT
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createTransaction — DEPOSIT")
    class Deposit {

        private CreateTransactionRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateTransactionRequest(
                    TransactionType.DEPOSIT, BigDecimal.valueOf(200),
                    10L, null, null, "desc", null);
        }

        @Test
        @DisplayName("success — balance increases by deposit amount")
        void success() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
            when(transactionMapper.toTransactionResponse(eq(savedTx), any(BigDecimal.class)))
                    .thenReturn(txResponse);

            TransactionResponse result = service.createTransaction(request);

            assertThat(result.type()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(account.getBalance()).isEqualByComparingTo("1200");
            verify(accountRepository).save(account);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("throws InvalidTransactionException when accountId is null")
        void nullAccountId() {
            CreateTransactionRequest noId = new CreateTransactionRequest(
                    TransactionType.DEPOSIT, BigDecimal.valueOf(200),
                    null, null, null, "desc", null);
            when(securityContextService.getCurrentUser()).thenReturn(user);

            assertThatThrownBy(() -> service.createTransaction(noId))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Account Id is required");
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account not owned by user")
        void accountNotFound() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createTransaction(request))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when user is not authenticated")
        void unauthenticated() {
            when(securityContextService.getCurrentUser())
                    .thenThrow(new IllegalStateException("Authentication required"));

            assertThatThrownBy(() -> service.createTransaction(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Authentication required");
        }
    }

    // -----------------------------------------------------------------------
    // WITHDRAWAL
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createTransaction — WITHDRAWAL")
    class Withdrawal {

        @Test
        @DisplayName("success — balance decreases by withdrawal amount")
        void success() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.WITHDRAWAL, BigDecimal.valueOf(300),
                    10L, null, null, "desc", null);
            TransactionResponse withdrawResponse = new TransactionResponse(
                    100L, TransactionType.WITHDRAWAL, BigDecimal.valueOf(300),
                    10L, null, null, BigDecimal.valueOf(700),
                    "desc", Instant.now(), Instant.now());

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
            when(transactionMapper.toTransactionResponse(eq(savedTx), any(BigDecimal.class)))
                    .thenReturn(withdrawResponse);

            TransactionResponse result = service.createTransaction(request);

            assertThat(result.type()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(account.getBalance()).isEqualByComparingTo("700");
        }

        @Test
        @DisplayName("throws InsufficientFundsException when balance is lower than amount")
        void insufficientFunds() {
            account.setBalance(BigDecimal.valueOf(50));
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.WITHDRAWAL, BigDecimal.valueOf(200),
                    10L, null, null, "desc", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.of(account));

            assertThatThrownBy(() -> service.createTransaction(request))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient funds");

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidTransactionException when accountId is null")
        void nullAccountId() {
            CreateTransactionRequest noId = new CreateTransactionRequest(
                    TransactionType.WITHDRAWAL, BigDecimal.valueOf(100),
                    null, null, null, "desc", null);
            when(securityContextService.getCurrentUser()).thenReturn(user);

            assertThatThrownBy(() -> service.createTransaction(noId))
                    .isInstanceOf(InvalidTransactionException.class);
        }
    }

    // -----------------------------------------------------------------------
    // TRANSFER (via CreateTransactionRequest / internal IDs)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createTransaction — TRANSFER")
    class Transfer {

        private Account targetAccount;

        @BeforeEach
        void setUp() {
            targetAccount = new Account();
            targetAccount.setId(20L);
            targetAccount.setUser(new User("alice", "alice@test.com", "pwd"));
            targetAccount.setCurrencyType(CurrencyType.EUR);
            targetAccount.setBalance(BigDecimal.valueOf(500));
            targetAccount.setActive(true);
        }

        @Test
        @DisplayName("success — source debited, target credited")
        void success() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.TRANSFER, BigDecimal.valueOf(200),
                    null, 10L, 20L, "pay", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.findByIdAndActiveTrue(20L))
                    .thenReturn(Optional.of(targetAccount));
            when(transactionRepository.save(any())).thenReturn(savedTx);
            when(transactionMapper.toTransactionResponse(eq(savedTx), any()))
                    .thenReturn(txResponse);

            service.createTransaction(request);

            assertThat(account.getBalance()).isEqualByComparingTo("800");
            assertThat(targetAccount.getBalance()).isEqualByComparingTo("700");
            verify(accountRepository, times(2)).save(any(Account.class));
        }

        @Test
        @DisplayName("throws InvalidTransactionException when source and target are same")
        void sameAccount() {
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.TRANSFER, BigDecimal.valueOf(100),
                    null, 10L, 10L, "self", null);
            when(securityContextService.getCurrentUser()).thenReturn(user);

            assertThatThrownBy(() -> service.createTransaction(request))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("must differ");
        }

        @Test
        @DisplayName("throws InvalidTransactionException when currencies differ")
        void currencyMismatch() {
            targetAccount.setCurrencyType(CurrencyType.USD);
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.TRANSFER, BigDecimal.valueOf(100),
                    null, 10L, 20L, "pay", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.findByIdAndActiveTrue(20L))
                    .thenReturn(Optional.of(targetAccount));

            assertThatThrownBy(() -> service.createTransaction(request))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("same currency");
        }

        @Test
        @DisplayName("throws InsufficientFundsException when source balance too low")
        void insufficientFunds() {
            account.setBalance(BigDecimal.valueOf(50));
            CreateTransactionRequest request = new CreateTransactionRequest(
                    TransactionType.TRANSFER, BigDecimal.valueOf(200),
                    null, 10L, 20L, "pay", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.findByIdAndActiveTrue(20L))
                    .thenReturn(Optional.of(targetAccount));

            assertThatThrownBy(() -> service.createTransaction(request))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("throws InvalidTransactionException when source or target id is null")
        void missingIds() {
            CreateTransactionRequest noTarget = new CreateTransactionRequest(
                    TransactionType.TRANSFER, BigDecimal.valueOf(100),
                    null, 10L, null, "pay", null);
            when(securityContextService.getCurrentUser()).thenReturn(user);

            assertThatThrownBy(() -> service.createTransaction(noTarget))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("required");
        }
    }

    // -----------------------------------------------------------------------
    // TRANSFER via accountIdentification
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("transferUsingAccountIdentification")
    class TransferByIdentification {

        private Account targetAccount;

        @BeforeEach
        void setUp() {
            targetAccount = new Account();
            targetAccount.setId(20L);
            targetAccount.setUser(new User("alice", "alice@test.com", "pwd"));
            targetAccount.setCurrencyType(CurrencyType.EUR);
            targetAccount.setBalance(BigDecimal.valueOf(500));
            targetAccount.setAccountIdentification("A918Z1");
            targetAccount.setActive(true);
        }

        @Test
        @DisplayName("success — balances updated via accountIdentification")
        void success() {
            TransferRequest request = new TransferRequest(
                    "K042M3", "A918Z1", BigDecimal.valueOf(100), "pay", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByAccountIdentificationAndUser_IdAndActiveTrue("K042M3", 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.findByAccountIdentificationAndActiveTrue("A918Z1"))
                    .thenReturn(Optional.of(targetAccount));
            when(transactionRepository.save(any())).thenReturn(savedTx);
            when(transactionMapper.toTransactionResponse(eq(savedTx), any())).thenReturn(txResponse);

            service.transferUsingAccountIdentification(request);

            assertThat(account.getBalance()).isEqualByComparingTo("900");
            assertThat(targetAccount.getBalance()).isEqualByComparingTo("600");
        }

        @Test
        @DisplayName("throws InvalidTransactionException when source and target identification are equal")
        void sameIdentification() {
            TransferRequest request = new TransferRequest(
                    "K042M3", "K042M3", BigDecimal.valueOf(100), "self", null);

            assertThatThrownBy(() -> service.transferUsingAccountIdentification(request))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("must differ");
        }

        @Test
        @DisplayName("throws AccountNotFoundException when source not found or not owned")
        void sourceNotFound() {
            TransferRequest request = new TransferRequest(
                    "K042M3", "A918Z1", BigDecimal.valueOf(100), "pay", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByAccountIdentificationAndUser_IdAndActiveTrue("K042M3", 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transferUsingAccountIdentification(request))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Source account");
        }

        @Test
        @DisplayName("throws AccountNotFoundException when target not found or inactive")
        void targetNotFound() {
            TransferRequest request = new TransferRequest(
                    "K042M3", "A918Z1", BigDecimal.valueOf(100), "pay", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByAccountIdentificationAndUser_IdAndActiveTrue("K042M3", 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.findByAccountIdentificationAndActiveTrue("A918Z1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transferUsingAccountIdentification(request))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Target account");
        }

        @Test
        @DisplayName("throws InsufficientFundsException when source balance too low")
        void insufficientFunds() {
            account.setBalance(BigDecimal.valueOf(50));
            TransferRequest request = new TransferRequest(
                    "K042M3", "A918Z1", BigDecimal.valueOf(200), "pay", null);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByAccountIdentificationAndUser_IdAndActiveTrue("K042M3", 1L))
                    .thenReturn(Optional.of(account));
            when(accountRepository.findByAccountIdentificationAndActiveTrue("A918Z1"))
                    .thenReturn(Optional.of(targetAccount));

            assertThatThrownBy(() -> service.transferUsingAccountIdentification(request))
                    .isInstanceOf(InsufficientFundsException.class);
        }
    }

    // -----------------------------------------------------------------------
    // listTransactionsForCurrentUser
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("listTransactionsForCurrentUser")
    class ListTransactions {

        @Test
        @DisplayName("success — returns mapped page")
        void success() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Transaction> page = new PageImpl<>(List.of(savedTx), pageable, 1);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(transactionRepository.findAllInvolvingAccountsOfUser(
                    eq(1L), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);
            when(transactionMapper.toDto(savedTx)).thenReturn(null);

            PagedTransactionsResponse result =
                    service.listTransactionsForCurrentUser(pageable, null, null, null);

            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws InvalidTransactionException when from is after to")
        void invalidDateRange() {
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate from = LocalDate.of(2026, 3, 31);
            LocalDate to   = LocalDate.of(2026, 3, 1);

            assertThatThrownBy(() ->
                    service.listTransactionsForCurrentUser(pageable, null, from, to))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("from must be before");
        }
    }
}