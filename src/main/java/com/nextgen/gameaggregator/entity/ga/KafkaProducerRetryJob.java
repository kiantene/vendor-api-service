package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nextgen.gameaggregator.core.retry.enums.RetryJobStatus;
import com.nextgen.gameaggregator.service.kafka.KafkaSerializerType;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Durable fallback record for a Kafka producer send that failed (GA-14578).
 *
 * <p>When a producer send fails — synchronously ({@code Failed to construct kafka producer} when the
 * Confluent Schema Registry is unavailable) or asynchronously (broker/ack) — the exact serialized
 * payload is enqueued here instead of dropped. {@code ga-retry-service} replays it once Kafka recovers.
 *
 * <p>Plain POJO written through the native Couchbase SDK (KV upsert by {@link #id}) into
 * {@code retry.kafka_producer_retry_jobs}. Its <b>contract deliberately mirrors {@code HttpRetryJob}</b>
 * so the {@code ga-retry-service} reader can select it with the same conventions it already uses for
 * http retries — {@code status} is the {@link RetryJobStatus} <i>name</i> (e.g. {@code "PENDING"}) and
 * scheduling uses {@code nextRunAt} (epoch millis), matching that service's
 * {@code WHERE status = "PENDING" AND nextRunAt <= now}. This service only <b>enqueues</b> (status
 * {@code PENDING}); replay, status transitions, and the move to {@code kafka_producer_retry_jobs_dlq}
 * are owned by {@code ga-retry-service} (GA-14749).
 */
@Data
public class KafkaProducerRetryJob {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    /** KV document key ({@code topic::dedupKey}) so repeated failures of the same message dedup. */
    @JsonProperty("id")
    private String id;

    @JsonProperty("createdAt")
    private String createdAt;
    @JsonProperty("createdTs")
    private long createdTs;

    /** Coarse source discriminator, mirroring {@code HttpRetryJob.origin}. */
    @JsonProperty("origin")
    private String origin;
    /** Optional correlation id (nullable — no natural trace id on the produce path today). */
    @JsonProperty("traceId")
    private String traceId;

    // ---- Kafka replay payload ----
    /** Destination topic — routes replay and is the recon filter key. */
    @JsonProperty("topic")
    private String topic;
    /** Original Kafka message key (nullable — preserved on replay so partitioning is unchanged). */
    @JsonProperty("messageKey")
    private String messageKey;
    /** Which template/serializer produced this — picks the template on replay. */
    @JsonProperty("serializerType")
    private KafkaSerializerType serializerType;
    /** Fully-qualified class of the payload, so JSON_SCHEMA payloads can be re-deserialized before re-send. */
    @JsonProperty("payloadClass")
    private String payloadClass;
    /** Serialized payload — the exact value we tried to send (JSON String for both templates). */
    @JsonProperty("payloadJson")
    private String payloadJson;

    // ---- Status & scheduling (ga-retry-service contract) ----
    /** {@code PENDING} on enqueue; ga-retry-service transitions it and moves exhausted jobs to the _dlq. */
    @JsonProperty("status")
    private RetryJobStatus status;
    @JsonProperty("attempts")
    private int attempts;
    /** Epoch millis the job becomes eligible for replay — matches ga-retry-service's {@code nextRunAt}. */
    @JsonProperty("nextRunAt")
    private long nextRunAt;
    /** Last send/replay failure (exception {@code toString()}, never null-losing). */
    @JsonProperty("lastError")
    private String lastError;

    public KafkaProducerRetryJob() {
        this.createdTs = System.currentTimeMillis();
        this.createdAt = DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(this.createdTs));
        this.attempts = 0;
        this.nextRunAt = this.createdTs;
        this.status = RetryJobStatus.PENDING;
    }
}
