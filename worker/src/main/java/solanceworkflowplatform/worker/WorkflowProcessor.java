package solanceworkflowplatform.worker;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(WorkflowProcessor.class);

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
        logger.info("Received workflow event from SQS");
        logger.debug("Raw event data: {}", raw);

        WorkflowEvent evt;
        try {
            JsonNode envelope = mapper.readTree(raw);
            evt = mapper.treeToValue(envelope.get("detail"), WorkflowEvent.class);
            logger.info("Parsed workflow event: type={}, eventId={}", evt.detailType(), evt.eventId());
        } catch (Exception e) {
            logger.error("Failed to parse workflow event", e);
            return;
        }

        switch (evt.detailType()) {
            case RegisterRequest.EVENT_TYPE:
                logger.info("Processing register request: eventId={}", evt.eventId());
                handleRegister(evt);
                break;
            case OpenAccountRequest.EVENT_TYPE:
                logger.info("Processing open account request: eventId={}", evt.eventId());
                handleOpenAccount(evt);
                break;
            case DepositRequest.EVENT_TYPE:
                logger.info("Processing deposit request: eventId={}", evt.eventId());
                handleDeposit(evt);
                break;
            case PaymentInstructionRequest.EVENT_TYPE:
                logger.info("Processing payment instruction request: eventId={}", evt.eventId());
                handlePayout(evt);
                break;
            default:
                logger.warn("Unknown event type: {}, eventId={}", evt.detailType(), evt.eventId());
        }

        // mark the workflow event as completed
        logger.info("Marking workflow event as completed: eventId={}", evt.eventId());
        updateStatus(evt.eventId(), "COMPLETED");
    }

    private void handleRegister(WorkflowEvent evt) {
        logger.debug("Executing registration logic for eventId={}", evt.eventId());
        // TODO: apply registration logic, e.g., create customer record
        logger.debug("Registration processing completed for eventId={}", evt.eventId());
    }

    private void handleOpenAccount(WorkflowEvent evt) {
        logger.debug("Executing account opening logic for eventId={}", evt.eventId());
        // TODO: apply account opening logic
        logger.debug("Account opening processing completed for eventId={}", evt.eventId());
    }

    private void handleDeposit(WorkflowEvent evt) {
        logger.debug("Executing deposit logic for eventId={}", evt.eventId());
        // TODO: apply deposit logic
        logger.debug("Deposit processing completed for eventId={}", evt.eventId());
    }

    private void handlePayout(WorkflowEvent evt) {
        logger.debug("Executing payout logic for eventId={}", evt.eventId());
        // TODO: apply payout logic
        logger.debug("Payout processing completed for eventId={}", evt.eventId());
    }

    private void updateStatus(String eventId, String status) {
        logger.debug("Updating status for eventId={} to status={}", eventId, status);

        Map<String,AttributeValue> key = Map.of(
                "eventId", AttributeValue.builder().s(eventId).build()
        );
        Map<String,AttributeValue> values = Map.of(
                ":s", AttributeValue.builder().s(status).build()
        );

        try {
            dynamo.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET #st = :s")
                    .expressionAttributeNames(Map.of("#st", "status"))
                    .expressionAttributeValues(values)
                    .build()
            );
            logger.debug("Successfully updated status for eventId={}", eventId);
        } catch (Exception e) {
            logger.error("Failed to update status for eventId={}", eventId, e);
            throw e;
        }
    }
}
