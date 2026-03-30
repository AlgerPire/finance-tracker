package com.finance_tracker.backend_server.account.service.impl;

import com.finance_tracker.backend_server.account.dto.response.AccountListResponse;
import com.finance_tracker.backend_server.account.dto.response.AccountResponse;
import com.finance_tracker.backend_server.account.dto.request.CreateAccountRequest;
import com.finance_tracker.backend_server.account.dto.request.ChangeAccountStatusRequest;
import com.finance_tracker.backend_server.account.dto.request.UpdateAccountRequest;
import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import com.finance_tracker.backend_server.account.mapper.AccountMapper;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.account.service.AccountService;
import com.finance_tracker.backend_server.account.support.AccountIdentificationGenerator;
import com.finance_tracker.backend_server.common.exception.AccountNotFoundException;
import com.finance_tracker.backend_server.common.exception.DuplicateAccountException;
import com.finance_tracker.backend_server.common.util.SecurityContextService;
import com.finance_tracker.backend_server.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    private final SecurityContextService securityContextService;

    private final AccountIdentificationGenerator accountIdentificationGenerator;

    private final AccountMapper accountMapper;


    @Autowired
    public AccountServiceImpl(
            AccountRepository accountRepository,
            SecurityContextService securityContextService,
            AccountIdentificationGenerator accountIdentificationGenerator, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.securityContextService = securityContextService;
        this.accountIdentificationGenerator = accountIdentificationGenerator;
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        User owner = securityContextService.getCurrentUser();
        accountExistsCheck(owner.getId(), request.accountType(), request.currencyType());
        Account account = new Account();
        account.setUser(owner);
        account.setAccountType(request.accountType());
        account.setBalance(request.initialBalance());
        account.setCurrencyType(request.currencyType());
        account.setAccountIdentification(accountIdentificationGenerator.generateUnique());
        account.setActive(true);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(Long accountId, UpdateAccountRequest request) {
        User owner = securityContextService.getCurrentUser();
        Account account = accountRepository.findByIdAndUser_Id(accountId, owner.getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        assertNoDuplicateAccountForUpdate(owner.getId(), account.getId(), request.accountType(), request.currencyType());
        account.setAccountType(request.accountType());
        account.setBalance(request.balance());
        account.setCurrencyType(request.currencyType());
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse changeAccountStatus(Long accountId, ChangeAccountStatusRequest request) {
        User owner = securityContextService.getCurrentUser();
        Account account = accountRepository.findByIdAndUser_Id(accountId, owner.getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        account.setActive(request.active());
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountListResponse listActiveAccounts() {
        User owner = securityContextService.getCurrentUser();
        List<AccountResponse> accounts = accountRepository.findAllByUser_IdAndActiveTrueOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(accountMapper::toDto)
                .toList();
        if (accounts.isEmpty()) {
            return new AccountListResponse(accounts, "You haven't created any account yet.");
        }
        return new AccountListResponse(accounts, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(Long accountId) {
        User owner = securityContextService.getCurrentUser();
        Account account = accountRepository.findByIdAndUser_IdAndActiveTrue(accountId, owner.getId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        return accountMapper.toDto(account);
    }

    private void assertNoDuplicateAccountForUpdate(
            Long ownerId, Long accountId, AccountType accountType, CurrencyType currencyType) {
        if (accountRepository.existsByUser_IdAndAccountTypeAndCurrencyTypeAndIdNot(
                ownerId, accountType, currencyType, accountId)) {
            throw new DuplicateAccountException(
                    "You already have a " + accountType + " account in " + currencyType);
        }
    }

    private void accountExistsCheck(Long ownerId, AccountType accountType, CurrencyType currencyType) {
        if (accountRepository.existsByUser_IdAndAccountTypeAndCurrencyType(ownerId, accountType, currencyType)) {
            throw new DuplicateAccountException(
                    "You already have a " + accountType + " account in " + currencyType);
        }
    }
}
