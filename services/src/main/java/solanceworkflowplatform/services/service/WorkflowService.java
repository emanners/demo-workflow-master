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

        // 1. write initial record to DynamoDB
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.builder().s(eventId).build());
        item.put("type", AttributeValue.builder().s(type).build());
        item.put("status", AttributeValue.builder().s("RECEIVED").build());

        logger.debug("Writing initial record to DynamoDB: table={}, eventId={}", tableName, eventId);
        try {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build()
            );
            logger.debug("Successfully wrote initial record to DynamoDB: eventId={}", eventId);
        } catch (Exception e) {
            logger.error("Failed to write initial record to DynamoDB: eventId={}", eventId, e);
            return Mono.error(new RuntimeException("Failed to write initial record to DynamoDB", e));
        }

        // Serialize the payload
        String detail;
        try {
            detail = mapper.writeValueAsString(payload);
            logger.info("Serialized payload for event: eventId={}", eventId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payload: eventId={}", eventId, e);
            return Mono.error(new RuntimeException("Failed to serialize payload", e));
        }

        if (useDirectSqs) {
            // Send directly to SQS
            try {
                // Get the queue URL
                String queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                        .queueUrl();

                // Create a message with metadata
                Map<String, String> messageAttributes = new HashMap<>();
                messageAttributes.put("eventId", eventId);
                messageAttributes.put("type", type);

                // Send the message to SQS
                logger.info("Sending message directly to SQS: queue={}, eventId={}", queueName, eventId);
                sqs.sendMessage(SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(detail)
                        .messageAttributes(Map.of(
                            "eventId", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(eventId)
                                .build(),
                            "type", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(type)
                                .build()
                        ))
                        .build());
                logger.info("Successfully sent message to SQS: eventId={}", eventId);
            } catch (Exception e) {
                logger.error("Failed to send message to SQS: eventId={}", eventId, e);
                return Mono.error(new RuntimeException("Failed to send message to SQS", e));
            }
        } else {
            // Publish to EventBridge
            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(eventBusName)
                    .source("com.solance.workflow")
                    .detailType(type)
                    .detail(detail)
                    .build();

            logger.info("Publishing event to EventBridge: eventBus={}, eventId={}", eventBusName, eventId);
            try {
                eb.putEvents(PutEventsRequest.builder()
                        .entries(entry)
                        .build()
                );
                logger.info("Successfully published event to EventBridge: eventId={}", eventId);
            } catch (Exception e) {
                logger.error("Failed to publish event to EventBridge: eventId={}", eventId, e);
                return Mono.error(new RuntimeException("Failed to publish event to EventBridge", e));
            }
        }

        logger.info("Successfully submitted workflow event: eventId={}", eventId);
        return Mono.just(eventId);
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
