package com.finance_tracker.backend_server.account.service;

import com.finance_tracker.backend_server.account.dto.response.AdminAccountResponse;
import com.finance_tracker.backend_server.account.dto.response.PagedAccountResponse;
import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import com.finance_tracker.backend_server.account.mapper.AccountMapper;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.account.service.impl.AdminAccountServiceImpl;
import com.finance_tracker.backend_server.common.exception.AccountNotFoundException;
import com.finance_tracker.backend_server.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAccountServiceImpl")
class AdminAccountServiceImplTest {

    @Mock AccountRepository accountRepository;
    @Mock AccountMapper accountMapper;

    @InjectMocks AdminAccountServiceImpl service;

    private User user;
    private Account account;
    private AdminAccountResponse adminAccountResponse;

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

        adminAccountResponse = new AdminAccountResponse(
                10L, "K042M3", "john", AccountType.SAVINGS,
                BigDecimal.valueOf(500), CurrencyType.EUR,
                true, Instant.now(), Instant.now());
    }

    // -----------------------------------------------------------------------
    // listAllAccounts
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("listAllAccounts")
    class ListAllAccounts {

        @Test
        @DisplayName("success — returns paginated accounts with correct metadata")
        void success() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

            when(accountRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(accountMapper.toAdminResponse(account)).thenReturn(adminAccountResponse);

            PagedAccountResponse result = service.listAllAccounts(pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.page()).isZero();
            assertThat(result.first()).isTrue();
            assertThat(result.last()).isTrue();
        }

        @Test
        @DisplayName("success — returns empty content when no accounts exist")
        void emptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(accountRepository.findAll(any(Pageable.class))).thenReturn(Page.empty(pageable));

            PagedAccountResponse result = service.listAllAccounts(pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }


    }

    // -----------------------------------------------------------------------
    // disableAccount
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("disableAccount")
    class DisableAccount {

        @Test
        @DisplayName("success — active account is disabled and response returned")
        void success() {
            AdminAccountResponse disabledResponse = new AdminAccountResponse(
                    10L, "K042M3", "john", AccountType.SAVINGS,
                    BigDecimal.valueOf(500), CurrencyType.EUR,
                    false, Instant.now(), Instant.now());

            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAdminResponse(account)).thenReturn(disabledResponse);

            AdminAccountResponse result = service.disableAccount(10L);

            assertThat(account.isActive()).isFalse();
            assertThat(result.active()).isFalse();
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("throws AccountNotFoundException when account does not exist")
        void accountNotFound() {
            when(accountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.disableAccount(99L))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalStateException when account is already inactive")
        void alreadyInactive() {
            account.setActive(false);
            when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> service.disableAccount(10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already inactive");

            verify(accountRepository, never()).save(any());
        }
    }
}
