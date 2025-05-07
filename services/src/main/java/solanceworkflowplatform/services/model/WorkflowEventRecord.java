package solanceworkflowplatform.services.model;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

/**
 * Record representing a workflow event stored in DynamoDB.
 */
public record WorkflowEventRecord(String eventId, String detailType, String status) {

    /** Map a DynamoDB item (field -> AttributeValue) into this record. */
    public static WorkflowEventRecord fromDynamo(Map<String, AttributeValue> item) {
        String eventId = null;
        String detailType = null;
        String status = null;

        AttributeValue eventIdAttr = item.get("eventId");
        if (eventIdAttr != null) {
            eventId = eventIdAttr.s();
        }

        AttributeValue typeAttr = item.get("type");
        if (typeAttr != null) {
            detailType = typeAttr.s();
        }

        AttributeValue statusAttr = item.get("status");
        if (statusAttr != null) {
            status = statusAttr.s();
        }

        return new WorkflowEventRecord(eventId, detailType, status);
    }
}
