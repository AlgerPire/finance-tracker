package com.finance_tracker.backend_server.account.entity;

import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import com.finance_tracker.backend_server.common.entity.AuditableEntity;
import com.finance_tracker.backend_server.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_accounts_account_identification", columnNames = "account_identification"))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // owner: user can own multiple accounts
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountType accountType;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Digits(integer = 17, fraction = 2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyType currencyType; // EUR, USD, GBP, LEK

    /**
     * External-facing reference: letter + 3 digits + letter (e.g. K042M), unique across all accounts.
     */
    @NotNull
    @Column(name = "account_identification", nullable = false, unique = true, length = 6)
    private String accountIdentification;

    @Column(nullable = false)
    private boolean active = true;
}
