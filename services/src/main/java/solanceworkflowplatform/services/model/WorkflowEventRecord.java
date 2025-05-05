package solanceworkflowplatform.services.model;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class WorkflowEventRecord {
    private String eventId;
    private String detailType;
    private String status;



    /** Map a DynamoDB item (field -> AttributeValue) into this POJO. */
    public static WorkflowEventRecord fromDynamo(Map<String, AttributeValue> item) {
        WorkflowEventRecord r = new WorkflowEventRecord();

        AttributeValue eventIdAttr = item.get("eventId");
        if (eventIdAttr != null) {
            r.setEventId(eventIdAttr.s());
        }

        AttributeValue typeAttr = item.get("type");
        if (typeAttr != null) {
            r.setDetailType(typeAttr.s());
        }

        AttributeValue statusAttr = item.get("status");
        if (statusAttr != null) {
            r.setStatus(statusAttr.s());
        }

        return r;
    }

    private void setStatus(String status) {
        this.status = status;
    }

    private void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    private void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getDetailType() {
        return detailType;
    }

    public String getStatus() {
        return status;
    }
}
