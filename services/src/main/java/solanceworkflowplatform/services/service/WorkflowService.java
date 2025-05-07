package solanceworkflowplatform.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import solanceworkflowplatform.services.model.WorkflowEvent;
import solanceworkflowplatform.services.model.WorkflowEventRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    private final DynamoDbClient dynamo;
    private final EventBridgeClient eb;
    private final SqsClient sqs;
    private final ObjectMapper mapper;
    private final String tableName;
    private final String eventBusName;
    private final String queueName;
    private final boolean useDirectSqs;

    public WorkflowService(
            DynamoDbClient dynamo,
            EventBridgeClient eb,
            SqsClient sqs,
            ObjectMapper mapper,
            @Value("${DDB_TABLE:solance-workflow}") String tableName,
            @Value("${EVENT_BUS:workflow-bus}") String eventBusName,
            @Value("${sqs.queue.workflow.name}") String queueName,
            @Value("${workflow.use-direct-sqs:true}") boolean useDirectSqs
    ) {
        this.dynamo = dynamo;
        this.eb = eb;
        this.sqs = sqs;
        this.mapper = mapper;
        this.tableName = tableName;
        this.eventBusName = eventBusName;
        this.queueName = queueName;
        this.useDirectSqs = useDirectSqs;

        logger.info("WorkflowService initialized with useDirectSqs={}", useDirectSqs);
    }

    public Mono<String> submit(String type, Object payload) {
        String eventId = UUID.randomUUID().toString();
        logger.info("Submitting new workflow event: type={}, eventId={}", type, eventId);
        logger.debug("Workflow payload: {}", payload);

        // 1. Persist a “RECEIVED” record
        try {
            writeInitialRecord(eventId, type);
        } catch (Exception ex) {
            return Mono.error(ex);
        }

        // 2. Serialise payload once (needed for EB path)
        String detailJson;
        try {
            detailJson = serializePayload(payload, eventId);
        } catch (Exception ex) {
            return Mono.error(ex);
        }

        // 3. Dispatch
        try {
            if (useDirectSqs) {
                sendToSqs(eventId, type, payload);
            } else {
                publishToEventBridge(eventId, type, detailJson);
            }
        } catch (Exception ex) {
            return Mono.error(ex);
        }

        logger.info("Successfully submitted workflow event: eventId={}", eventId);
        return Mono.just(eventId);
    }

    /* --------------------------------------------------------------------- */
    /* Private helpers                                                       */
    /* --------------------------------------------------------------------- */

    private void writeInitialRecord(String eventId, String type) {
        Map<String, AttributeValue> item = Map.of(
                "eventId", AttributeValue.builder().s(eventId).build(),
                "type",    AttributeValue.builder().s(type).build(),
                "status",  AttributeValue.builder().s("RECEIVED").build()
        );

        logger.debug("Writing initial record to DynamoDB: table={}, eventId={}", tableName, eventId);
        dynamo.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build()
        );
    }

    private String serializePayload(Object payload, String eventId) throws JsonProcessingException {
        String json = mapper.writeValueAsString(payload);
        logger.info("Serialized payload for event: eventId={}", eventId);
        return json;
    }

    private void sendToSqs(String eventId, String type, Object payload) throws Exception {
        String queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build()).queueUrl();

        WorkflowEvent workflowEvent = new WorkflowEvent(
                eventId,
                type,
                mapper.valueToTree(payload) // to JsonNode
        );
        String message = mapper.writeValueAsString(workflowEvent);

        logger.info("Sending message directly to SQS: queue={}, eventId={}", queueName, eventId);
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .messageAttributes(Map.of(
                        "eventId", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                .dataType("String").stringValue(eventId).build(),
                        "type", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                .dataType("String").stringValue(type).build()
                ))
                .build());
        logger.info("Successfully sent message to SQS: eventId={}", eventId);
    }

    private void publishToEventBridge(String eventId, String type, String detailJson) {
        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .source("com.solance.workflow")
                .detailType(type)
                .detail(detailJson)
                .build();

        logger.info("Publishing event to EventBridge: eventBus={}, eventId={}", eventBusName, eventId);
        eb.putEvents(PutEventsRequest.builder()
                .entries(entry)
                .build());
        logger.info("Successfully published event to EventBridge: eventId={}", eventId);
    }

    public Flux<WorkflowEventRecord> listEvents() {
        logger.info("Listing all workflow events");

        return Flux.defer(() -> {
            logger.debug("Scanning DynamoDB table: {}", tableName);
            try {
                ScanResponse resp = dynamo.scan(ScanRequest.builder()
                        .tableName(tableName)
                        .build());

                logger.debug("Retrieved {} workflow events from DynamoDB", resp.items().size());
                return Flux.fromIterable(resp.items())
                        .map(item -> {
                            WorkflowEventRecord record = WorkflowEventRecord.fromDynamo(item);
                            logger.trace("Mapped DynamoDB item to WorkflowEventRecord: eventId={}, detailType={}, status={}", 
                                    record.eventId(), record.detailType(), record.status());
                            return record;
                        });
            } catch (Exception e) {
                logger.error("Failed to scan DynamoDB table: {}", tableName, e);
                return Flux.error(e);
            }
        });
    }
}
