package com.finance_tracker.backend_server.transaction.service;

import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.entity.Transaction;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import com.finance_tracker.backend_server.transaction.mapper.TransactionMapper;
import com.finance_tracker.backend_server.transaction.repository.TransactionRepository;
import com.finance_tracker.backend_server.transaction.service.impl.AdminTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTransactionServiceImpl")
class AdminTransactionServiceImplTest {

    @Mock TransactionRepository transactionRepository;
    @Mock TransactionMapper transactionMapper;

    @InjectMocks AdminTransactionServiceImpl service;

    private Transaction tx;

    @BeforeEach
    void setUp() {
        tx = new Transaction();
        tx.setId(1L);
        tx.setTransactionAt(Instant.now());
    }

    @Nested
    @DisplayName("listAllTransactions")
    class ListAllTransactions {

        @Test
        @DisplayName("success — returns paginated transactions with no filters")
        void noFilters() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Transaction> page = new PageImpl<>(List.of(tx), pageable, 1);

            when(transactionRepository.findAllForAdmin(anyList(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);
            when(transactionMapper.toDto(tx)).thenReturn(null);

            PagedTransactionsResponse result =
                    service.listAllTransactions(pageable, null, null, null, null);

            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("success — filter by type passes only that type to repository")
        void filterByType() {
            Pageable pageable = PageRequest.of(0, 20);
            when(transactionRepository.findAllForAdmin(anyList(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listAllTransactions(pageable, TransactionType.DEPOSIT, null, null, null);

            verify(transactionRepository).findAllForAdmin(
                    eq(List.of(TransactionType.DEPOSIT)), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("success — filter by userId is forwarded to repository")
        void filterByUserId() {
            Pageable pageable = PageRequest.of(0, 20);
            when(transactionRepository.findAllForAdmin(anyList(), eq(5L), any(), any(), any()))
                    .thenReturn(Page.empty());

            service.listAllTransactions(pageable, null, 5L, null, null);

            verify(transactionRepository).findAllForAdmin(anyList(), eq(5L), any(), any(), any());
        }

        @Test
        @DisplayName("clamps page size to MAX_PAGE_SIZE=100 when oversized request")
        void clampPageSize() {
            Pageable oversized = PageRequest.of(0, 500);
            when(transactionRepository.findAllForAdmin(anyList(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            service.listAllTransactions(oversized, null, null, null, null);

            verify(transactionRepository).findAllForAdmin(
                    anyList(), any(), any(), any(),
                    argThat(p -> p.getPageSize() == 100));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when from is after to")
        void invalidDateRange() {
            LocalDate from = LocalDate.of(2026, 3, 31);
            LocalDate to   = LocalDate.of(2026, 3, 1);
            Pageable pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() ->
                    service.listAllTransactions(pageable, null, null, from, to))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("from must be before");
        }

        @Test
        @DisplayName("returns empty content when no transactions match")
        void empty() {
            Pageable pageable = PageRequest.of(0, 20);
            when(transactionRepository.findAllForAdmin(anyList(), any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            PagedTransactionsResponse result =
                    service.listAllTransactions(pageable, null, null, null, null);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }
}