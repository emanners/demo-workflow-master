package services.model;

/**
 * Represents a register customer request.
 */
public record RegisterRequest(
    String userId,
    String fullName,
    String email
) {
    public static final String EVENT_TYPE = "RegisterCustomer";
}
