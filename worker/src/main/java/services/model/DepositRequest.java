package services.model;

import java.time.Instant;

/**
 * Represents a deposit request (pay-in).
 */
public record DepositRequest(
    String userId,
    String accountId,
    String currency,
    double amount,
    Instant transactedAt
) {
    public static final String EVENT_TYPE = "Deposit";
}
