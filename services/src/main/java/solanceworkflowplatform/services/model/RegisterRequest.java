package solanceworkflowplatform.services.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

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
