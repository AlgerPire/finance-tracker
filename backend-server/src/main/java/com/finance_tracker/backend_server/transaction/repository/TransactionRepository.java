package com.finance_tracker.backend_server.transaction.repository;

import com.finance_tracker.backend_server.transaction.entity.Transaction;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<@NonNull Transaction, @NonNull Long> {

    @EntityGraph(attributePaths = {"account", "sourceAccount", "targetAccount"})
    @Query(
            value = """
                    SELECT t FROM Transaction t
                    WHERE ((t.account IS NOT NULL AND t.account.user.id = :userId)
                       OR (t.sourceAccount IS NOT NULL AND t.sourceAccount.user.id = :userId)
                       OR (t.targetAccount IS NOT NULL AND t.targetAccount.user.id = :userId))
                      AND (:type IS NULL OR t.type = :type)
                    """,
            countQuery = """
                    SELECT count(t) FROM Transaction t
                    WHERE ((t.account IS NOT NULL AND t.account.user.id = :userId)
                       OR (t.sourceAccount IS NOT NULL AND t.sourceAccount.user.id = :userId)
                       OR (t.targetAccount IS NOT NULL AND t.targetAccount.user.id = :userId))
                      AND (:type IS NULL OR t.type = :type)
                    """)
    Page<Transaction> findAllInvolvingAccountsOfUser(
            @Param("userId") Long userId, @Param("type") TransactionType type, Pageable pageable);
}