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

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<@NonNull Transaction, @NonNull Long> {

    @EntityGraph(attributePaths = {"account", "sourceAccount", "targetAccount"})
    @Query(
            value = """
                SELECT t FROM Transaction t
                WHERE ((t.account IS NOT NULL AND t.account.user.id = :userId)
                   OR (t.sourceAccount IS NOT NULL AND t.sourceAccount.user.id = :userId)
                   OR (t.targetAccount IS NOT NULL AND t.targetAccount.user.id = :userId))
                  AND (CAST(:type AS string) IS NULL OR t.type = :type)
                  AND (CAST(:from AS java.time.Instant) IS NULL OR t.transactionAt >= :from)
                  AND (CAST(:to AS java.time.Instant) IS NULL OR t.transactionAt <= :to)
                """,
            countQuery = """
                SELECT count(t) FROM Transaction t
                WHERE ((t.account IS NOT NULL AND t.account.user.id = :userId)
                   OR (t.sourceAccount IS NOT NULL AND t.sourceAccount.user.id = :userId)
                   OR (t.targetAccount IS NOT NULL AND t.targetAccount.user.id = :userId))
                  AND (CAST(:type AS string) IS NULL OR t.type = :type)
                  AND (CAST(:from AS java.time.Instant) IS NULL OR t.transactionAt >= :from)
                  AND (CAST(:to AS java.time.Instant) IS NULL OR t.transactionAt <= :to)
                """)
    Page<Transaction> findAllInvolvingAccountsOfUser(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @EntityGraph(attributePaths = {"account", "sourceAccount", "targetAccount"})
    @Query(
            value = """
                SELECT t FROM Transaction t
                WHERE t.type IN :types
                  AND (:userId IS NULL OR t.user.id = :userId)
                  AND t.transactionAt >= :fromBound
                  AND t.transactionAt <= :toBound
                """,
            countQuery = """
                SELECT count(t) FROM Transaction t
                WHERE t.type IN :types
                  AND (:userId IS NULL OR t.user.id = :userId)
                  AND t.transactionAt >= :fromBound
                  AND t.transactionAt <= :toBound
                """)
    Page<Transaction> findAllForAdmin(
            @Param("types") List<TransactionType> types,
            @Param("userId") Long userId,
            @Param("fromBound") Instant fromBound,
            @Param("toBound") Instant toBound,
            Pageable pageable);
}