package com.finance_tracker.backend_server.account.support;

import com.finance_tracker.backend_server.account.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Produces unique codes: one uppercase letter, four digits (0000–9999), one uppercase letter (e.g. A0042Z).
 * Total space: 26 × 10,000 × 26 = 6,760,000 possible codes.
 */
@Component
public class AccountIdentificationGenerator {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int DIGIT_BOUND = 10_000;
    private static final double EXHAUSTION_THRESHOLD = 0.85;
    private static final long TOTAL_SPACE = 26L * DIGIT_BOUND * 26;
    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountIdentificationGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generateUnique() {
        checkSpaceUtilization();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = buildCandidate();
            if (!accountRepository.existsByAccountIdentification(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique account identification after " + MAX_ATTEMPTS + " attempts");
    }

    private void checkSpaceUtilization() {
        long used = accountRepository.count();
        if (used >= TOTAL_SPACE * EXHAUSTION_THRESHOLD) {
            throw new IllegalStateException(
                    "Account ID space is over " + (int)(EXHAUSTION_THRESHOLD * 100) + "% exhausted (" + used + "/" + TOTAL_SPACE + ")"
            );
        }
    }

    private String buildCandidate() {
        return randomLetter() + String.format("%04d", random.nextInt(DIGIT_BOUND)) + randomLetter();
    }

    private char randomLetter() {
        return LETTERS.charAt(random.nextInt(LETTERS.length()));
    }
}