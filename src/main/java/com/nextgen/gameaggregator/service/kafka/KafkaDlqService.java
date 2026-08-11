package com.nextgen.gameaggregator.service.kafka;

import com.couchbase.client.java.Collection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.KafkaProducerRetryJob;
import com.nextgen.gameaggregator.util.StackTraceUtils;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic, opt-in fallback for Kafka producer sends (GA-14578).
 *
 * <p>{@link #sendWithFallback} produces a message and, if the send fails either synchronously
 * (producer construction) or asynchronously (broker/ack), persists the exact serialized payload
 * to Couchbase ({@code retry.kafka_producer_retry_jobs}) instead of dropping it. This keeps
 * financial bet-history data recoverable during a Kafka / Schema Registry outage.
 *
 * <p>The service owns template selection so a caller can never pair the wrong template with the
 * wrong {@link KafkaSerializerType}. This class only <em>enqueues</em> — replay of persisted records
 * (and moving exhausted ones to {@code kafka_producer_retry_jobs_dlq}) is handled by
 * {@code ga-retry-service}, mirroring its http-retry machinery.
 */
@Service
@Slf4j
public class KafkaDlqService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** Coarse source discriminator on the persisted job (mirrors HttpRetryJob.origin). */
    private static final String ORIGIN = "KAFKA_PRODUCER";

    /** Fallback engaged — a failed send was buffered to Couchbase (early warning of Kafka trouble). */
    private static final String METRIC_PERSISTED = "kafka.producer.fallback.persisted";
    /** Double failure — Kafka send AND the Couchbase buffer write both failed. SRE alert: potential loss. */
    private static final String METRIC_PERSIST_FAILED = "kafka.producer.fallback.persist.failed";

    // Small bounded pool that runs the blocking Couchbase write OFF the Kafka producer I/O thread.
    private static final int PERSIST_CORE_THREADS = 2;
    private static final int PERSIST_MAX_THREADS = 4;
    private static final int PERSIST_QUEUE_CAPACITY = 1000;

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;
    private final Collection kafkaProducerRetryJobsCollection;
    private final MeterRegistry meterRegistry;
    private final ExecutorService persistExecutor = createPersistExecutor();

    @Autowired
    public KafkaDlqService(KafkaTemplate<String, String> stringKafkaTemplate,
                           KafkaTemplate<String, Object> jsonSchemaKafkaTemplate,
                           @Qualifier("kafkaProducerRetryJobsCollection") Collection kafkaProducerRetryJobsCollection,
                           MeterRegistry meterRegistry) {
        this.stringKafkaTemplate = stringKafkaTemplate;
        this.jsonSchemaKafkaTemplate = jsonSchemaKafkaTemplate;
        this.kafkaProducerRetryJobsCollection = kafkaProducerRetryJobsCollection;
        this.meterRegistry = meterRegistry;
    }

    private static ExecutorService createPersistExecutor() {
        AtomicInteger threadNo = new AtomicInteger();
        // AbortPolicy → a saturated queue throws RejectedExecutionException, which submitPersist()
        // catches WITH topic context (record-and-drop to reconciliation); it never blocks the caller.
        return new ThreadPoolExecutor(
                PERSIST_CORE_THREADS, PERSIST_MAX_THREADS, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(PERSIST_QUEUE_CAPACITY),
                runnable -> {
                    Thread t = new Thread(runnable, "kafka-dlq-persist-" + threadNo.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    @PreDestroy
    void shutdown() {
        persistExecutor.shutdown();
        try {
            // Let in-flight/queued fallback writes (financial records) finish before teardown —
            // the persist threads are daemons and would otherwise be killed mid-write.
            if (!persistExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("KafkaDlqService persist executor did not drain within 30s; forcing shutdown");
                persistExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            persistExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Send {@code value} to {@code topic}; on any send failure, persist it for later replay.
     *
     * @param topic       destination topic
     * @param kafkaKey    Kafka message key — pass {@code null} to preserve keyless partitioning
     * @param dedupKey    stable business key used to build the dedup document id (e.g.
     *                    {@code vendorId:vendorBetId:roundId}); falls back to a payload hash if null
     * @param value       payload — a JSON {@code String} for {@link KafkaSerializerType#STRING},
     *                    or the object to JSON-Schema serialize for {@link KafkaSerializerType#JSON_SCHEMA}
     * @param serializerType which template to use
     */
    public void sendWithFallback(String topic, String kafkaKey, String dedupKey,
                                 Object value, KafkaSerializerType serializerType) {
        try {
            // Switch EXPRESSION with no default → adding a new KafkaSerializerType without handling it
            // here is a COMPILE error, not a silent runtime no-op that would drop a financial record.
            CompletableFuture<?> future = switch (serializerType) {
                case STRING -> stringKafkaTemplate.send(topic, kafkaKey, (String) value);
                case JSON_SCHEMA -> jsonSchemaKafkaTemplate.send(topic, kafkaKey, value);
            };
            future.exceptionally(throwable -> {
                submitPersist(topic, kafkaKey, dedupKey, value, serializerType, throwable);
                return null;
            });
        } catch (Exception e) {
            // Synchronous failure — most importantly "Failed to construct kafka producer".
            submitPersist(topic, kafkaKey, dedupKey, value, serializerType, e);
        }
    }

    /**
     * Run {@link #persist} on the bounded {@link #persistExecutor}, NOT inline. The async failure path's
     * {@code exceptionally} runs on Spring Kafka's producer I/O thread; doing the blocking Couchbase write
     * there would stall the (shared) producer under sustained failure. On executor saturation we
     * record-and-drop to the persist-failed metric and rely on reconciliation (GA-14749) to recover from
     * the settle source of truth — we never block the producer thread.
     */
    private void submitPersist(String topic, String kafkaKey, String dedupKey,
                               Object value, KafkaSerializerType serializerType, Throwable cause) {
        try {
            persistExecutor.execute(() -> persist(topic, kafkaKey, dedupKey, value, serializerType, cause));
        } catch (RejectedExecutionException rejected) {
            meterRegistry.counter(METRIC_PERSIST_FAILED, "topic", topic).increment();
            log.error("KafkaProducerRetryJob PERSIST REJECTED — buffer executor saturated; relying on reconciliation. topic={}, dedupKey={}, kafkaCause={}",
                    topic, dedupKey, cause);
        }
    }

    /**
     * Persist a failed payload to the fallback store. Never throws — a store failure is logged so it
     * cannot propagate back into the Kafka producer callback thread.
     */
    void persist(String topic, String kafkaKey, String dedupKey,
                 Object value, KafkaSerializerType serializerType, Throwable cause) {
        if (value == null) {
            // Defensive: nothing to buffer. Record it rather than NPE on value.getClass().
            meterRegistry.counter(METRIC_PERSIST_FAILED, "topic", topic).increment();
            log.error("KafkaProducerRetryJob persist called with null value — nothing to buffer. topic={}, dedupKey={}, kafkaCause={}",
                    topic, dedupKey, cause);
            return;
        }
        try {
            String payloadJson = (value instanceof String s) ? s : OBJECT_MAPPER.writeValueAsString(value);

            // ctor sets status=PENDING, attempts=0, createdTs/createdAt, nextRunAt=now (ga-retry-service contract).
            KafkaProducerRetryJob job = new KafkaProducerRetryJob();
            job.setId(buildId(topic, dedupKey, payloadJson));
            job.setOrigin(ORIGIN);
            job.setTopic(topic);
            job.setMessageKey(kafkaKey);
            job.setSerializerType(serializerType);
            job.setPayloadClass(value.getClass().getName());
            job.setPayloadJson(payloadJson);
            // toString() (not getMessage(), which is null for e.g. NPE) so the exception type is always kept.
            job.setLastError(cause != null ? cause.toString() : null);

            // Native SDK KV upsert (dedup: a repeat failure of the same message overwrites its doc).
            // No TTL by design — these are financial records; they must not auto-expire before recon.
            // ga-retry-service replays/cleans up; a retention policy is decided there (see GA-14749).
            kafkaProducerRetryJobsCollection.upsert(job.getId(), job);

            meterRegistry.counter(METRIC_PERSISTED, "topic", topic).increment();
            log.warn("KafkaProducerRetryJob persisted: topic={}, id={}, cause={}",
                    topic, job.getId(), job.getLastError());
        } catch (Exception e) {
            // Last resort — do NOT rethrow into the producer callback thread.
            // Both Kafka and Couchbase failed → this record is at risk until reconciliation re-derives
            // it from the persisted GameRound/GameTransaction. Metric + stable prefix so SRE is paged.
            meterRegistry.counter(METRIC_PERSIST_FAILED, "topic", topic).increment();
            // Log BOTH failures: the Couchbase error (e) and the original Kafka error (cause) that
            // triggered the fallback — otherwise SRE only sees the Couchbase side of a double failure.
            log.error("KafkaProducerRetryJob PERSIST FAILED — potential data loss until reconciliation. topic={}, dedupKey={}, kafkaCause={}, {}",
                    topic, dedupKey, cause, StackTraceUtils.getStackTraceAsString(e));
        }
    }

    /**
     * {@code topic::dedupKey}, or {@code topic::<sha256(payload)>} when no usable business key is given.
     * A dedupKey is unusable if it is null/blank or contains a {@code "null"} segment (from a null
     * component, e.g. an unpersisted entity's id) — those would collapse distinct records onto one doc.
     * The fallback is a deterministic SHA-256 (collision-resistant, unlike 32-bit {@code hashCode()}, and
     * still idempotent — the same payload maps to the same id, so repeated failures dedup rather than pile up).
     */
    private String buildId(String topic, String dedupKey, String payloadJson) {
        String discriminator = isUsableDedupKey(dedupKey) ? dedupKey : sha256Hex(payloadJson);
        return topic + "::" + discriminator;
    }

    private static boolean isUsableDedupKey(String dedupKey) {
        if (dedupKey == null || dedupKey.isBlank()) {
            return false;
        }
        // Per-segment check (not substring): a null component concatenates to a literal "null" segment;
        // a legit key merely containing "null" (e.g. an "annulled" roundId) must stay usable.
        for (String segment : dedupKey.split("::", -1)) {
            if (segment.isBlank() || "null".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private static String sha256Hex(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated on every JVM; unreachable in practice.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
