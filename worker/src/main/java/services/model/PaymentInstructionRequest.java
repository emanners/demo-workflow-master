package services.model;

import java.time.Instant;

/**
 * Represents a payment instruction request (pay-out).
 */
public record PaymentInstructionRequest(
    String userId,
    String accountId,
    String currency,
    double amount,
    Instant transactedAt,
    String beneficiaryIban,
    String paymentRef,
    String purposeRef
) {
    public static final String EVENT_TYPE = "Payout";
}
