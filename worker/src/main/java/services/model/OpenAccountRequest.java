package services.model;

/**
 * Represents an open account request.
 */
public record OpenAccountRequest(
    String userId,
    String accountType,
    String currency
) {
    public static final String EVENT_TYPE = "OpenAccount";
}
