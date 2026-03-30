package com.finance_tracker.backend_server.account.repository;

import com.finance_tracker.backend_server.account.entity.Account;
import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Account} entity operations.
 */
@Repository
public interface AccountRepository extends JpaRepository<@NonNull Account, @NonNull Long> {

    boolean existsByUser_IdAndAccountTypeAndCurrencyType(Long userId, AccountType accountType, CurrencyType currencyType);

    Optional<Account> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndAccountTypeAndCurrencyTypeAndIdNot(
            Long userId, AccountType accountType, CurrencyType currencyType, Long id);

    Optional<Account> findByIdAndUser_IdAndActiveTrue(Long id, Long userId);

    List<Account> findAllByUser_IdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    boolean existsByAccountIdentification(String accountIdentification);

    Optional<Account> findByIdAndActiveTrue(Long id);

    Optional<Account> findByAccountIdentificationAndUser_IdAndActiveTrue(
            String accountIdentification, Long userId);

    Optional<Account> findByAccountIdentificationAndActiveTrue(String accountIdentification);
}
