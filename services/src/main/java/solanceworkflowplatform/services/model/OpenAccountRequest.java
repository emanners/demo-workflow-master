package solanceworkflowplatform.services.model;

// Open Account Request
public class OpenAccountRequest {
    public static final String EVENT_TYPE = "OpenAccount";
    private String userId;
    private String accountType;
    private String currency;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}