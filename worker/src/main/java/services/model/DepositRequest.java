package services.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

// Deposit Request (Pay-in)
public class DepositRequest {
    public static final String EVENT_TYPE = "Deposit";
    private String userId;
    private String accountId;
    private String currency;
    private double amount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Instant transactedAt;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Instant getTransactedAt() { return transactedAt; }
    public void setTransactedAt(Instant transactedAt) { this.transactedAt = transactedAt; }
}