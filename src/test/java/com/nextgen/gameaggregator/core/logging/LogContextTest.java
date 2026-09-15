package com.nextgen.gameaggregator.core.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for GA-14989: the log payload fields (apiBody/body/response/apiResponse) are
 * typed Object and were previously serialized as-is, so the same field could land in OpenSearch as a
 * nested object on one document and as a String on another. OpenSearch dynamic mapping is
 * first-doc-wins per daily index, so the mismatch caused mapper_parsing_exception and dropped logs.
 * These tests lock in that toJson() always emits these fields as a stable JSON String.
 */
class LogContextTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode toNode(LogContext ctx) throws Exception {
        return MAPPER.readTree(ctx.toJson());
    }

    @Test
    void apiBodyIsAlwaysStringRegardlessOfCallerType() throws Exception {
        // Caller sets a structured object (e.g. the endround settlement POJO/Map).
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("roundId", "R-123");
        structured.put("winLoss", 500);
        LogContext objectCtx = new LogContext();
        objectCtx.setApiBody(structured);

        // Caller sets a raw JSON string (e.g. a launch / adapter body).
        LogContext stringCtx = new LogContext();
        stringCtx.setApiBody("{\"roundId\":\"R-123\",\"winLoss\":500}");

        JsonNode fromObject = toNode(objectCtx).get("apiBody");
        JsonNode fromString = toNode(stringCtx).get("apiBody");

        // Core regression: both must serialize to the SAME JSON type (textual) — never
        // object-on-one-doc and string-on-another, which is what broke the OpenSearch mapping.
        assertTrue(fromObject.isTextual(), "apiBody from a POJO/Map must be a JSON string");
        assertTrue(fromString.isTextual(), "apiBody from a String must stay a JSON string");
        assertFalse(fromObject.isObject(), "apiBody must never be serialized as a nested object");

        // The stringified object must still be a faithful JSON encoding of what was set.
        JsonNode reparsed = MAPPER.readTree(fromObject.asText());
        assertEquals("R-123", reparsed.get("roundId").asText());
        assertEquals(500, reparsed.get("winLoss").asInt());
    }

    @Test
    void stringPayloadsPassThroughUnchanged() throws Exception {
        LogContext ctx = new LogContext();
        ctx.setApiBody("raw-api-body");
        ctx.setBody("raw-request-body");

        JsonNode node = toNode(ctx);

        assertEquals("raw-api-body", node.get("apiBody").asText());
        assertEquals("raw-request-body", node.get("body").asText());
    }

    @Test
    void allPayloadFieldsAreTextualWhenSetWithObjects() throws Exception {
        Map<String, Object> payload = Map.of("k", "v");
        LogContext ctx = new LogContext();
        ctx.setBody(payload);
        ctx.setResponse(payload);
        ctx.setApiBody(payload);
        ctx.setApiResponse(payload);

        JsonNode node = toNode(ctx);

        for (String field : new String[]{"body", "response", "apiBody", "apiResponse"}) {
            assertTrue(node.get(field).isTextual(), field + " must be serialized as a JSON string");
        }
    }
}
