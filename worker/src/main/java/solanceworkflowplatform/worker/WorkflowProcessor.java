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
            evt = parseWorkflowEvent(raw);
        } catch (Exception ex) {
            logger.error("Failed to parse workflow event", ex);
            return;
        }

        try {
            routeEvent(evt);
            updateStatus(evt.eventId(), "COMPLETED");
            logger.info("Workflow event processed successfully: eventId={}", evt.eventId());
        } catch (Exception ex) {
            logger.error("Error while handling workflow event: eventId={}", evt.eventId(), ex);
            updateStatus(evt.eventId(), "FAILED");
        }
    }

    /* ---------- helpers ---------------------------------------------------- */

    private WorkflowEvent parseWorkflowEvent(String raw) throws Exception {
        JsonNode root = mapper.readTree(raw);

        // 1) Direct-to-SQS: payload already matches WorkflowEvent
        if (root.has("eventId") && root.has("detailType")) {
            return mapper.treeToValue(root, WorkflowEvent.class);
        }

        // 2) EventBridge envelope: unwrap "detail"
        if (root.has("detail")) {
            JsonNode inner = root.get("detail");
            if (inner.has("eventId") && inner.has("detailType")) {
                return mapper.treeToValue(inner, WorkflowEvent.class);
            }
        }
        throw new IllegalArgumentException("Unsupported event shape");
    }

    private void routeEvent(WorkflowEvent evt) {
        logger.info("Routing workflow event: type={}, eventId={}", evt.detailType(), evt.eventId());

        switch (evt.detailType()) {
            case RegisterRequest.EVENT_TYPE -> {
                logger.debug("Handling RegisterRequest");
                handleRegister(evt);
            }
            case OpenAccountRequest.EVENT_TYPE -> {
                logger.debug("Handling OpenAccountRequest");
                handleOpenAccount(evt);
            }
            case DepositRequest.EVENT_TYPE -> {
                logger.debug("Handling DepositRequest");
                handleDeposit(evt);
            }
            case PaymentInstructionRequest.EVENT_TYPE -> {
                logger.debug("Handling PaymentInstructionRequest");
                handlePayout(evt);
            }
            default -> logger.warn("Unknown event type: {}, eventId={}", evt.detailType(), evt.eventId());
        }
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
