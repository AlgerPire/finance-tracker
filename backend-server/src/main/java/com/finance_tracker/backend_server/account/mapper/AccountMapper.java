package com.finance_tracker.backend_server.account.mapper;

import com.finance_tracker.backend_server.account.dto.response.AccountResponse;
import com.finance_tracker.backend_server.account.dto.response.AdminAccountResponse;
import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.common.mapper.BaseEntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper extends BaseEntityMapper<AccountResponse, Account> {


    @Override
    AccountResponse toDto(Account account);

    @Override
    default Account toEntity(AccountResponse dto) {
        throw new UnsupportedOperationException("AccountResponse cannot be mapped back to Account");
    }

    @Mapping(target = "ownerUsername", source = "user.username")
    AdminAccountResponse toAdminResponse(Account account);
}