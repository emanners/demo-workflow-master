package solanceworkflowplatform.services.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a generic workflow event with metadata and payload.
 */
public class WorkflowEvent {
    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("detailType")
    private String detailType;

    @JsonProperty("detail")
    private JsonNode detail;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDetailType() {
        return detailType;
    }

    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    public JsonNode getDetail() {
        return detail;
    }

    public void setDetail(JsonNode detail) {
        this.detail = detail;
    }
}
