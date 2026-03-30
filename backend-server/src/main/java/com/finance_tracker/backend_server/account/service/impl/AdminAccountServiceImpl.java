package com.finance_tracker.backend_server.account.service.impl;

import com.finance_tracker.backend_server.account.dto.response.AdminAccountResponse;
import com.finance_tracker.backend_server.account.dto.response.PagedAccountResponse;
import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.mapper.AccountMapper;
import com.finance_tracker.backend_server.account.repository.AccountRepository;
import com.finance_tracker.backend_server.account.service.AdminAccountService;
import com.finance_tracker.backend_server.common.exception.AccountNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminAccountServiceImpl implements AdminAccountService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Autowired
    public AdminAccountServiceImpl(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedAccountResponse listAllAccounts(Pageable pageable) {
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        Pageable effective = PageRequest.of(pageable.getPageNumber(), size, sort);
        Page<Account> page = accountRepository.findAll(effective);
        List<AdminAccountResponse> content =
                page.getContent().stream().map(accountMapper::toAdminResponse).toList();
        return new PagedAccountResponse(
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
    public AdminAccountResponse disableAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        if (!account.isActive()) {
            throw new IllegalStateException("Account is already inactive!");
        }
        account.setActive(false);
        return accountMapper.toAdminResponse(accountRepository.save(account));
    }

}
