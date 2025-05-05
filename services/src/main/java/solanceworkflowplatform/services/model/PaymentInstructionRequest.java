package solanceworkflowplatform.services.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

// Payment Instruction Request (Pay-out)
public class PaymentInstructionRequest {
    public static final String EVENT_TYPE = "Payout";
    private String userId;
    private String accountId;
    private String currency;
    private double amount;
    private Instant transactedAt;
    private String beneficiaryIban;
    private String paymentRef;
    private String purposeRef;

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

    public String getBeneficiaryIban() { return beneficiaryIban; }
    public void setBeneficiaryIban(String beneficiaryIban) { this.beneficiaryIban = beneficiaryIban; }

    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    public String getPurposeRef() { return purposeRef; }
    public void setPurposeRef(String purposeRef) { this.purposeRef = purposeRef; }
}
