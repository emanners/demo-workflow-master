package solanceworkflowplatform.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import solanceworkflowplatform.services.model.PaymentInstructionRequest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ServicesApplicationTests {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void testInstantDeserialization() throws Exception {
        // The JSON string with the format that was causing the issue
        String json = "{\"userId\":\"user123\",\"accountId\":\"acc456\",\"currency\":\"USD\",\"amount\":100.0,\"transactedAt\":\"2025-05-05T16:00:00Z\",\"beneficiaryIban\":\"IBAN123\",\"paymentRef\":\"REF123\",\"purposeRef\":\"PURPOSE123\"}";

        // Deserialize the JSON into a PaymentInstructionRequest object
        PaymentInstructionRequest request = objectMapper.readValue(json, PaymentInstructionRequest.class);

        // Verify that the deserialization worked correctly
        assertNotNull(request);
        assertNotNull(request.getTransactedAt());
        assertEquals(Instant.parse("2025-05-05T16:00:00Z"), request.getTransactedAt());

        // Verify other fields were also deserialized correctly
        assertEquals("user123", request.getUserId());
        assertEquals("acc456", request.getAccountId());
        assertEquals("USD", request.getCurrency());
        assertEquals(100.0, request.getAmount());
        assertEquals("IBAN123", request.getBeneficiaryIban());
        assertEquals("REF123", request.getPaymentRef());
        assertEquals("PURPOSE123", request.getPurposeRef());
    }
}
