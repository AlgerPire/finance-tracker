package com.finance_tracker.backend_server.transaction.mapper;

import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.common.mapper.BaseEntityMapper;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionListResponse;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionResponse;
import com.finance_tracker.backend_server.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper extends BaseEntityMapper<TransactionListResponse, Transaction> {


    @Override
    @Mapping(target = "accountId",                   source = "account.id")
    @Mapping(target = "currencyType",                source = "account",              qualifiedByName = "accountToCurrencyString")
    @Mapping(target = "sourceAccountId",             source = "sourceAccount.id")
    @Mapping(target = "sourceAccountName",           source = "sourceAccount.user.username")
    @Mapping(target = "sourceAccountIdentification", source = "sourceAccount.accountIdentification")
    @Mapping(target = "targetAccountId",             source = "targetAccount.id")
    @Mapping(target = "targetAccountName",           source = "targetAccount.user.username")
    @Mapping(target = "targetAccountIdentification", source = "targetAccount.accountIdentification")
    TransactionListResponse toDto(Transaction transaction);

    @Override
    default Transaction toEntity(TransactionListResponse dto) {
        throw new UnsupportedOperationException("TransactionListResponse cannot be mapped back to Transaction");
    }


    @Mapping(target = "accountId",           source = "account.id")
    @Mapping(target = "sourceAccountId",     source = "sourceAccount.id")
    @Mapping(target = "targetAccountId",     source = "targetAccount.id")
    @Mapping(target = "accountBalanceAfter", ignore = true)
    TransactionResponse toTransactionResponse(Transaction transaction);

    default TransactionResponse toTransactionResponse(Transaction transaction, BigDecimal accountBalanceAfter) {
        TransactionResponse base = toTransactionResponse(transaction);
        return new TransactionResponse(
                base.id(),
                base.type(),
                base.amount(),
                base.accountId(),
                base.sourceAccountId(),
                base.targetAccountId(),
                accountBalanceAfter,
                base.description(),
                base.transactionAt(),
                base.createdAt());
    }


    @Named("accountToCurrencyString")
    static String accountToCurrencyString(Account account) {
        return account != null ? account.getCurrencyType().toString() : null;
    }
}