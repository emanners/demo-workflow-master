package solanceworkflowplatform.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import solanceworkflowplatform.services.model.WorkflowEventRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {
    private final DynamoDbClient dynamo;
    private final EventBridgeClient eb;
    private final ObjectMapper mapper;
    private final String tableName;
    private final String eventBusName;

    public WorkflowService(
            DynamoDbClient dynamo,
            EventBridgeClient eb,
            ObjectMapper mapper,
            @Value("${DDB_TABLE:solance-workflow}") String tableName,
            @Value("${EVENT_BUS:workflow-bus}") String eventBusName
    ) {
        this.dynamo = dynamo;
        this.eb = eb;
        this.mapper = mapper;
        this.tableName = tableName;
        this.eventBusName = eventBusName;
    }

    public Mono<String> submit(String type, Object payload) {
        String eventId = UUID.randomUUID().toString();

        // 1. write initial record to DynamoDB
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.builder().s(eventId).build());
        item.put("type", AttributeValue.builder().s(type).build());
        item.put("status", AttributeValue.builder().s("RECEIVED").build());

        dynamo.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build()
        );

        // 2. publish event to EventBridge
        String detail;
        try {
            detail = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to serialize payload", e));
        }

        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .source("com.solance.workflow")
                .detailType(type)
                .detail(detail)
                .build();

        eb.putEvents(PutEventsRequest.builder()
                .entries(entry)
                .build()
        );

        return Mono.just(eventId);
    }

    public Flux<WorkflowEventRecord> listEvents() {
        return Flux.defer(() -> {
            ScanResponse resp = dynamo.scan(ScanRequest.builder()
                    .tableName(tableName)
                    .build());
            return Flux.fromIterable(resp.items())
                    .map(WorkflowEventRecord::fromDynamo);
        });
    }
}
