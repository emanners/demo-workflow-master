package solanceworkflowplatform.worker;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import services.model.*;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import io.awspring.cloud.sqs.annotation.SqsListener;



import java.util.Map;

@Component
public class WorkflowProcessor {
    private final DynamoDbClient dynamo;
    private final ObjectMapper mapper;
    private final String tableName;

    public WorkflowProcessor(
            DynamoDbClient dynamo,
            ObjectMapper mapper,
            @Value("${DDB_TABLE:solance-workflow}") String tableName
    ) {
        this.dynamo = dynamo;
        this.mapper = mapper;
        this.tableName = tableName;
    }

    @SqsListener("${sqs.queue.workflow.name}")
    public void onEvent(String raw) {
        WorkflowEvent evt;
        try {
            JsonNode envelope = mapper.readTree(raw);
            evt = mapper.treeToValue(envelope.get("detail"), WorkflowEvent.class);
        } catch (Exception e) {
            // log and skip
            return;
        }

        switch (evt.getDetailType()) {
            case RegisterRequest.EVENT_TYPE:
                handleRegister(evt);
                break;
            case OpenAccountRequest.EVENT_TYPE:
                handleOpenAccount(evt);
                break;
            case DepositRequest.EVENT_TYPE:
                handleDeposit(evt);
                break;
            case PaymentInstructionRequest.EVENT_TYPE:
                handlePayout(evt);
                break;
            default:
                // unknown event
        }

        // mark the workflow event as completed
        updateStatus(evt.getEventId(), "COMPLETED");
    }

    private void handleRegister(WorkflowEvent evt) {
        // TODO: apply registration logic, e.g., create customer record
    }

    private void handleOpenAccount(WorkflowEvent evt) {
        // TODO: apply account opening logic
    }

    private void handleDeposit(WorkflowEvent evt) {
        // TODO: apply deposit logic
    }

    private void handlePayout(WorkflowEvent evt) {
        // TODO: apply payout logic
    }

    private void updateStatus(String eventId, String status) {
        Map<String,AttributeValue> key = Map.of(
                "eventId", AttributeValue.builder().s(eventId).build()
        );
        Map<String,AttributeValue> values = Map.of(
                ":s", AttributeValue.builder().s(status).build()
        );

        dynamo.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .updateExpression("SET #st = :s")
                .expressionAttributeNames(Map.of("#st", "status"))
                .expressionAttributeValues(values)
                .build()
        );
    }
}
