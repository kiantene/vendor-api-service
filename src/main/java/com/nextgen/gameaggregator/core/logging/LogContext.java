package com.nextgen.gameaggregator.core.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.util.UuidUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogContext {
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private final Map<String, Object> extraFields = new LinkedHashMap<>(); // LinkedHashMap to maintain field ordering
    private String time;
    private String type;
    private String url;
    private String traceId;
    private String logGroup;

    private HttpMethod method;

    // Raw request body received from the client (e.g., operator system)
    private Object body;

    // Raw response body returned to the client
    private Object response;
    private long start;
    private long end;
    private long timeTaken;

    /**
     * Target URL of the outbound API request to the external system
     */
    private String apiUrl;
    /**
     * Outbound request payload sent to external systems (e.g., game vendor APIs)
     */
    private Object apiBody;

    /**
     * Inbound response payload received from external systems
     */
    private Object apiResponse;
    private long apiStart;
    private long apiEnd;
    private long apiTimeTaken;
    private int apiStatusCode;
    private String exception;
    private String rootCause;
    private String errorMessage;
    private String stackTrace;
    private int status;
    private String vendorClassName;
    private Integer vendorId;
    private Integer agentId;
    private String username; // always refers to agent player username

    public LogContext() {
        this.logGroup = "general";
        this.traceId = UuidUtil.newUuidV7StringRaw();
        this.start = System.currentTimeMillis();
        this.time = this.formatTimestamp(this.start);
    }

    public void setEnd() {
        this.end = System.currentTimeMillis();
        this.timeTaken = this.end - this.start;
    }

    public void setException(Exception ex) {
        setException(ex.getClass().getSimpleName());
        setErrorMessage(ex.getMessage());
        setStackTrace(getStackTrace(ex));

        if (ex instanceof RuntimeException && ex.getCause() != null) {
            Throwable cause = ex.getCause();

            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            setRootCause(cause.getClass().getSimpleName());
        }
    }

    public void setException(String ex) {
        this.exception = ex;
        this.status = -1;
    }

    public void put(String key, Object value) {
        extraFields.put(key, value);
    }
    public void delete(String key) {
        extraFields.remove(key);
    }

    public static void putField(String key, Object value) {
        LogContext context = LogContextHolder.get();
        if (context != null) {
            context.put(key, value);
        }
    }

    public Object get(String key) {
        return extraFields.get(key);
    }

    public boolean exists(String key) {
        return extraFields.containsKey(key);
    }

    private String formatTimestamp(Long timestamp) {
        // Convert timestamp to Instant
        Instant instant = Instant.ofEpochMilli(timestamp);
        // Convert Instant to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        // Define the formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

        return dateTime.format(formatter) + "Z";
    }

    public String toJson() {
        try {
            Map<String, Object> base = new LinkedHashMap<>();
            base.put("time", time);
            base.put("logGroup", logGroup);
            base.put("traceId", traceId);
            base.put("type", type);
            base.put("agentId", agentId);
            base.put("vendorId", vendorId);
            base.put("username", username);
            base.put("url", url);
            base.put("start", start);
            base.put("end", end);
            base.put("timeTaken", timeTaken);
            base.put("body", body);
            base.put("response", response);
            base.put("apiUrl", apiUrl);
            base.put("apiBody", apiBody);
            base.put("apiResponse", apiResponse);
            base.put("apiStart", apiStart);
            base.put("apiEnd", apiEnd);
            base.put("apiTimeTaken", apiTimeTaken);
            base.put("apiStatusCode", apiStatusCode);
            base.put("status", status);

            // Exception
            base.put("exception", exception);
            base.put("rootCause", rootCause);
            base.put("errorMessage", errorMessage);
            if (log.isDebugEnabled()) {
                base.put("method", method.name());
                base.put("stackTrace", stackTrace);
            }
            base.putAll(extraFields);

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            return mapper.writeValueAsString(base);

        } catch (JsonProcessingException jsonProcessingException) {
            return this.toString();
        }
    }

    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Create a defensive copy of this LogContext.
     * - Preserves traceId/start/time (overwrites constructor defaults).
     * - Deep-copies extraFields (new LinkedHashMap).
     * - Copies references for body/response/apiBody/apiResponse (shallow).
     */
    public LogContext copy() {
        LogContext clone = new LogContext();

        clone.traceId = this.traceId;
        clone.start = this.start;
        clone.time = this.time;
        clone.type = this.type;
        clone.url = this.url;
        clone.logGroup = this.logGroup;
        clone.method = this.method;

        // Payloads
        clone.body = this.body;
        clone.response = this.response;

        // Timing
        clone.end = this.end;
        clone.timeTaken = this.timeTaken;

        // Outbound API details
        clone.apiUrl = this.apiUrl;
        clone.apiBody = this.apiBody;
        clone.apiResponse = this.apiResponse;
        clone.apiStart = this.apiStart;
        clone.apiEnd = this.apiEnd;
        clone.apiTimeTaken = this.apiTimeTaken;
        clone.apiStatusCode = this.apiStatusCode;

        // Exception fields
        clone.exception = this.exception;
        clone.rootCause = this.rootCause;
        clone.errorMessage = this.errorMessage;
        clone.stackTrace = this.stackTrace;

        // Status & identifiers
        clone.status = this.status;
        clone.vendorClassName = this.vendorClassName;
        clone.vendorId = this.vendorId;
        clone.agentId = this.agentId;
        clone.username = this.username;

        // Extra fields (deep copy of the map)
        clone.extraFields.putAll(this.extraFields);

        return clone;
    }
}
