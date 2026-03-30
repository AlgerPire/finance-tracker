package com.finance_tracker.backend_server.account.service;

import com.finance_tracker.backend_server.account.dto.request.ChangeAccountStatusRequest;
import com.finance_tracker.backend_server.account.dto.request.CreateAccountRequest;
import com.finance_tracker.backend_server.account.dto.request.UpdateAccountRequest;
import com.finance_tracker.backend_server.account.dto.response.AccountListResponse;
import com.finance_tracker.backend_server.account.dto.response.AccountResponse;
import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import com.finance_tracker.backend_server.account.mapper.AccountMapper;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.account.service.impl.AccountServiceImpl;
import com.finance_tracker.backend_server.account.support.AccountIdentificationGenerator;
import com.finance_tracker.backend_server.common.exception.AccountNotFoundException;
import com.finance_tracker.backend_server.common.exception.DuplicateAccountException;
import com.finance_tracker.backend_server.common.util.SecurityContextService;
import com.finance_tracker.backend_server.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl")
class AccountServiceImplTest {

    @Mock AccountRepository accountRepository;
    @Mock SecurityContextService securityContextService;
    @Mock AccountIdentificationGenerator accountIdentificationGenerator;
    @Mock AccountMapper accountMapper;

    @InjectMocks AccountServiceImpl service;

    private User user;
    private Account account;
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "encoded_pwd");
        user.setId(1L);

        account = new Account();
        account.setId(10L);
        account.setUser(user);
        account.setAccountType(AccountType.SAVINGS);
        account.setCurrencyType(CurrencyType.EUR);
        account.setBalance(BigDecimal.valueOf(500));
        account.setAccountIdentification("K042M3");
        account.setActive(true);

        accountResponse = new AccountResponse(
                10L, "K042M3", AccountType.SAVINGS,
                BigDecimal.valueOf(500), CurrencyType.EUR,
                true, Instant.now(), Instant.now());
    }

    // -----------------------------------------------------------------------
    // createAccount
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        private CreateAccountRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateAccountRequest(
                    AccountType.SAVINGS, BigDecimal.valueOf(500), CurrencyType.EUR);
        }

        @Test
        @DisplayName("success — account saved with correct fields and response returned")
        void success() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.existsByUser_IdAndAccountTypeAndCurrencyType(
                    1L, AccountType.SAVINGS, CurrencyType.EUR)).thenReturn(false);
            when(accountIdentificationGenerator.generateUnique()).thenReturn("K042M3");
            when(accountRepository.save(any(Account.class))).thenReturn(account);
            when(accountMapper.toDto(account)).thenReturn(accountResponse);

            AccountResponse result = service.createAccount(request);

            assertThat(result.accountType()).isEqualTo(AccountType.SAVINGS);
            assertThat(result.currencyType()).isEqualTo(CurrencyType.EUR);
            assertThat(result.balance()).isEqualByComparingTo("500");
            verify(accountRepository).save(argThat(a ->
                    a.isActive()
                            && a.getUser().equals(user)
                            && a.getAccountIdentification().equals("K042M3")));
        }

        @Test
        @DisplayName("throws DuplicateAccountException when same type+currency already exists")
        void duplicateAccount() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.existsByUser_IdAndAccountTypeAndCurrencyType(
                    1L, AccountType.SAVINGS, CurrencyType.EUR)).thenReturn(true);

            assertThatThrownBy(() -> service.createAccount(request))
                    .isInstanceOf(DuplicateAccountException.class)
                    .hasMessageContaining("SAVINGS")
                    .hasMessageContaining("EUR");

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalStateException when user is not authenticated")
        void unauthenticated() {
            when(securityContextService.getCurrentUser())
                    .thenThrow(new IllegalStateException("Authentication required"));

            assertThatThrownBy(() -> service.createAccount(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Authentication required");

            verify(accountRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // updateAccount
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("updateAccount")
    class UpdateAccount {

        private UpdateAccountRequest request;

        @BeforeEach
        void setUp() {
            request = new UpdateAccountRequest(
                    AccountType.SAVINGS, BigDecimal.valueOf(750), CurrencyType.EUR);
        }

        @Test
        @DisplayName("success — account updated with new fields and response returned")
        void success() {
            Account updatedAccount = new Account();
            updatedAccount.setId(10L);
            updatedAccount.setBalance(BigDecimal.valueOf(750));

            AccountResponse updatedResponse = new AccountResponse(
                    10L, "K042M3", AccountType.SAVINGS,
                    BigDecimal.valueOf(750), CurrencyType.EUR,
                    true, Instant.now(), Instant.now());

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(account));
            when(accountRepository.existsByUser_IdAndAccountTypeAndCurrencyTypeAndIdNot(
                    1L, AccountType.SAVINGS, CurrencyType.EUR, 10L)).thenReturn(false);
            when(accountRepository.save(account)).thenReturn(updatedAccount);
            when(accountMapper.toDto(updatedAccount)).thenReturn(updatedResponse);

            AccountResponse result = service.updateAccount(10L, request);

            assertThat(result.balance()).isEqualByComparingTo("750");
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account not owned by user")
        void accountNotFound() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAccount(10L, request))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateAccountException when another account with same type+currency exists")
        void duplicateOnUpdate() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(account));
            when(accountRepository.existsByUser_IdAndAccountTypeAndCurrencyTypeAndIdNot(
                    1L, AccountType.SAVINGS, CurrencyType.EUR, 10L)).thenReturn(true);

            assertThatThrownBy(() -> service.updateAccount(10L, request))
                    .isInstanceOf(DuplicateAccountException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalStateException when user is not authenticated")
        void unauthenticated() {
            when(securityContextService.getCurrentUser())
                    .thenThrow(new IllegalStateException("Authentication required"));

            assertThatThrownBy(() -> service.updateAccount(10L, request))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -----------------------------------------------------------------------
    // changeAccountStatus
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("changeAccountStatus")
    class ChangeAccountStatus {

        @Test
        @DisplayName("success — deactivates an active account")
        void deactivate() {
            AccountResponse deactivatedResponse = new AccountResponse(
                    10L, "K042M3", AccountType.SAVINGS,
                    BigDecimal.valueOf(500), CurrencyType.EUR,
                    false, Instant.now(), Instant.now());

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toDto(account)).thenReturn(deactivatedResponse);

            AccountResponse result =
                    service.changeAccountStatus(10L, new ChangeAccountStatusRequest(false));

            assertThat(account.isActive()).isFalse();
            assertThat(result.active()).isFalse();
        }

        @Test
        @DisplayName("success — reactivates an inactive account")
        void reactivate() {
            account.setActive(false);

            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toDto(account)).thenReturn(accountResponse);

            service.changeAccountStatus(10L, new ChangeAccountStatusRequest(true));

            assertThat(account.isActive()).isTrue();
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account not owned by user")
        void accountNotFound() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.changeAccountStatus(10L, new ChangeAccountStatusRequest(false)))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalStateException when user is not authenticated")
        void unauthenticated() {
            when(securityContextService.getCurrentUser())
                    .thenThrow(new IllegalStateException("Authentication required"));

            assertThatThrownBy(() ->
                    service.changeAccountStatus(10L, new ChangeAccountStatusRequest(false)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -----------------------------------------------------------------------
    // listActiveAccounts
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("listActiveAccounts")
    class ListActiveAccounts {

        @Test
        @DisplayName("success — returns accounts with null message when list is non-empty")
        void withAccounts() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findAllByUser_IdAndActiveTrueOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(account));
            when(accountMapper.toDto(account)).thenReturn(accountResponse);

            AccountListResponse result = service.listActiveAccounts();

            assertThat(result.accounts()).hasSize(1);
            assertThat(result.message()).isNull();
        }

        @Test
        @DisplayName("success — returns empty list with hint message when user has no accounts")
        void noAccounts() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findAllByUser_IdAndActiveTrueOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            AccountListResponse result = service.listActiveAccounts();

            assertThat(result.accounts()).isEmpty();
            assertThat(result.message()).contains("haven't created any account");
        }

        @Test
        @DisplayName("throws IllegalStateException when user is not authenticated")
        void unauthenticated() {
            when(securityContextService.getCurrentUser())
                    .thenThrow(new IllegalStateException("Authentication required"));

            assertThatThrownBy(() -> service.listActiveAccounts())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -----------------------------------------------------------------------
    // getAccountDetails
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("getAccountDetails")
    class GetAccountDetails {

        @Test
        @DisplayName("success — returns account details for the authenticated owner")
        void success() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.of(account));
            when(accountMapper.toDto(account)).thenReturn(accountResponse);

            AccountResponse result = service.getAccountDetails(10L);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.accountIdentification()).isEqualTo("K042M3");
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account not owned or inactive")
        void accountNotFoundOrInactive() {
            when(securityContextService.getCurrentUser()).thenReturn(user);
            when(accountRepository.findByIdAndUser_IdAndActiveTrue(10L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAccountDetails(10L))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when user is not authenticated")
        void unauthenticated() {
            when(securityContextService.getCurrentUser())
                    .thenThrow(new IllegalStateException("Authentication required"));

            assertThatThrownBy(() -> service.getAccountDetails(10L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
