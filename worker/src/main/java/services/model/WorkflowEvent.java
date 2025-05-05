package services.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a generic workflow event with metadata and payload.
 */
public record WorkflowEvent(String eventId, String detailType, JsonNode detail) {
}
