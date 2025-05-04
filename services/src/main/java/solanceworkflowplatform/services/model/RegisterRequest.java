package solanceworkflowplatform.services.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

// Register Customer Request
public class RegisterRequest {
    public static final String EVENT_TYPE = "RegisterCustomer";
    private String userId;
    private String fullName;
    private String email;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
