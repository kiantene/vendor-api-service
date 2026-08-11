package com.nextgen.gameaggregator.service.kafka;

import com.couchbase.client.java.Collection;
import com.nextgen.gameaggregator.core.retry.enums.RetryJobStatus;
import com.nextgen.gameaggregator.entity.ga.KafkaProducerRetryJob;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaDlqServiceTest {

    private static final String TOPIC = "topic_bet_history_preprocessing_v3";
    // gaBetId-based dedup key, "::"-separated to match the codebase docId convention.
    private static final String DEDUP_KEY = "10::018f-ga-bet-uuid-v7::ROUND-1";

    @Mock
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Mock
    private KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;
    @Mock
    private Collection kafkaProducerRetryJobsCollection;

    private MeterRegistry meterRegistry;
    private KafkaDlqService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new KafkaDlqService(stringKafkaTemplate, jsonSchemaKafkaTemplate,
                kafkaProducerRetryJobsCollection, meterRegistry);
    }

    // ---------------- persist ----------------

    @Test
    void persist_buildsDeterministicDocWithExpectedFields() {
        service.persist(TOPIC, null, DEDUP_KEY, "{\"a\":1}", KafkaSerializerType.STRING,
                new RuntimeException("boom"));

        KafkaProducerRetryJob saved = captureSaved();
        assertThat(saved.getId()).isEqualTo(TOPIC + "::" + DEDUP_KEY);
        assertThat(saved.getTopic()).isEqualTo(TOPIC);
        assertThat(saved.getSerializerType()).isEqualTo(KafkaSerializerType.STRING);
        assertThat(saved.getPayloadClass()).isEqualTo(String.class.getName());
        assertThat(saved.getPayloadJson()).isEqualTo("{\"a\":1}");
        assertThat(saved.getLastError()).isEqualTo("java.lang.RuntimeException: boom");
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getStatus()).isEqualTo(RetryJobStatus.PENDING);
        assertThat(saved.getOrigin()).isEqualTo("KAFKA_PRODUCER");
        assertThat(saved.getMessageKey()).isNull();
        assertThat(saved.getNextRunAt()).isPositive();
        assertThat(saved.getCreatedTs()).isPositive();
    }

    @Test
    void persist_serializesNonStringPayloadToJson() {
        Payload payload = new Payload(7, "x");

        service.persist(TOPIC, "vp-user", DEDUP_KEY, payload, KafkaSerializerType.JSON_SCHEMA, null);

        KafkaProducerRetryJob saved = captureSaved();
        assertThat(saved.getPayloadClass()).isEqualTo(Payload.class.getName());
        assertThat(saved.getPayloadJson()).contains("\"id\":7").contains("\"name\":\"x\"");
        assertThat(saved.getMessageKey()).isEqualTo("vp-user");
        assertThat(saved.getLastError()).isNull();
    }

    @Test
    void persist_fallsBackToSha256WhenNoDedupKey() {
        String payload = "{\"a\":1}";
        service.persist(TOPIC, null, null, payload, KafkaSerializerType.STRING, null);

        KafkaProducerRetryJob saved = captureSaved();
        assertThat(saved.getId()).isEqualTo(TOPIC + "::" + sha256Hex(payload));
    }

    @Test
    void persist_treatsNullSegmentedDedupKeyAsAbsent() {
        // e.g. an unpersisted entity: "vendorId::null::roundId" must NOT collapse distinct records.
        String payload = "{\"a\":1}";
        service.persist(TOPIC, null, "10::null::ROUND-1", payload, KafkaSerializerType.STRING, null);

        KafkaProducerRetryJob saved = captureSaved();
        assertThat(saved.getId()).isEqualTo(TOPIC + "::" + sha256Hex(payload));
    }

    @Test
    void persist_nullCauseKeepsLastErrorNull_nonNullCauseKeepsType() {
        service.persist(TOPIC, null, DEDUP_KEY, "{\"a\":1}", KafkaSerializerType.STRING,
                new NullPointerException());
        // NPE has a null message; toString() still preserves the type.
        assertThat(captureSaved().getLastError()).isEqualTo("java.lang.NullPointerException");
    }

    @Test
    void persist_success_incrementsPersistedMetric() {
        service.persist(TOPIC, null, DEDUP_KEY, "{\"a\":1}", KafkaSerializerType.STRING, null);

        assertThat(meterRegistry.counter("kafka.producer.fallback.persisted", "topic", TOPIC).count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.find("kafka.producer.fallback.persist.failed").counter()).isNull();
    }

    @Test
    void persist_whenCouchbaseAlsoFails_doesNotThrowAndPagesViaFailureMetric() {
        when(kafkaProducerRetryJobsCollection.upsert(anyString(), any()))
                .thenThrow(new RuntimeException("couchbase down"));

        // Must not throw back into the producer callback thread.
        service.persist(TOPIC, null, DEDUP_KEY, "{\"a\":1}", KafkaSerializerType.STRING,
                new RuntimeException("kafka down"));

        assertThat(meterRegistry.counter("kafka.producer.fallback.persist.failed", "topic", TOPIC).count())
                .isEqualTo(1.0);
    }

    // ---------------- sendWithFallback ----------------

    @Test
    void sendWithFallback_string_success_doesNotPersist() {
        CompletableFuture<SendResult<String, String>> ok = CompletableFuture.completedFuture(null);
        when(stringKafkaTemplate.send(anyString(), any(), anyString())).thenReturn(ok);

        service.sendWithFallback(TOPIC, null, DEDUP_KEY, "{\"a\":1}", KafkaSerializerType.STRING);

        verify(stringKafkaTemplate).send(eq(TOPIC), any(), eq("{\"a\":1}"));
        verify(kafkaProducerRetryJobsCollection, never()).upsert(anyString(), any());
    }

    @Test
    void sendWithFallback_string_asyncFailure_persists() {
        CompletableFuture<SendResult<String, String>> failed =
                CompletableFuture.failedFuture(new RuntimeException("async-boom"));
        when(stringKafkaTemplate.send(anyString(), any(), anyString())).thenReturn(failed);

        service.sendWithFallback(TOPIC, null, DEDUP_KEY, "{\"a\":1}", KafkaSerializerType.STRING);

        // persist runs on the bounded executor (off the Kafka I/O thread) → await it.
        KafkaProducerRetryJob saved = captureSavedAsync();
        assertThat(saved.getLastError()).isEqualTo("java.lang.RuntimeException: async-boom");
        assertThat(saved.getSerializerType()).isEqualTo(KafkaSerializerType.STRING);
    }

    @Test
    void sendWithFallback_syncConstructionFailure_persists() {
        when(stringKafkaTemplate.send(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Failed to construct kafka producer"));

        service.sendWithFallback(TOPIC, null, DEDUP_KEY, "{\"a\":1}", KafkaSerializerType.STRING);

        KafkaProducerRetryJob saved = captureSavedAsync();
        assertThat(saved.getLastError()).isEqualTo("java.lang.RuntimeException: Failed to construct kafka producer");
    }

    @Test
    void sendWithFallback_jsonSchema_routesToJsonSchemaTemplate() {
        Payload payload = new Payload(1, "y");
        CompletableFuture<SendResult<String, Object>> ok = CompletableFuture.completedFuture(null);
        when(jsonSchemaKafkaTemplate.send(anyString(), any(), any())).thenReturn(ok);

        service.sendWithFallback(TOPIC, null, DEDUP_KEY, payload, KafkaSerializerType.JSON_SCHEMA);

        verify(jsonSchemaKafkaTemplate).send(eq(TOPIC), any(), eq(payload));
        verify(stringKafkaTemplate, never()).send(anyString(), any(), anyString());
        verify(kafkaProducerRetryJobsCollection, never()).upsert(anyString(), any());
    }

    @Test
    void sendWithFallback_jsonSchema_asyncFailure_persists() {
        Payload payload = new Payload(9, "z");
        CompletableFuture<SendResult<String, Object>> failed =
                CompletableFuture.failedFuture(new RuntimeException("schema-boom"));
        when(jsonSchemaKafkaTemplate.send(anyString(), any(), any())).thenReturn(failed);

        service.sendWithFallback(TOPIC, null, DEDUP_KEY, payload, KafkaSerializerType.JSON_SCHEMA);

        KafkaProducerRetryJob saved = captureSavedAsync();
        assertThat(saved.getSerializerType()).isEqualTo(KafkaSerializerType.JSON_SCHEMA);
        assertThat(saved.getPayloadClass()).isEqualTo(Payload.class.getName());
        assertThat(saved.getLastError()).isEqualTo("java.lang.RuntimeException: schema-boom");
    }

    private KafkaProducerRetryJob captureSaved() {
        ArgumentCaptor<KafkaProducerRetryJob> captor = ArgumentCaptor.forClass(KafkaProducerRetryJob.class);
        verify(kafkaProducerRetryJobsCollection).upsert(anyString(), captor.capture());
        return captor.getValue();
    }

    /** For the sendWithFallback paths, where persist() runs asynchronously on the bounded executor. */
    private KafkaProducerRetryJob captureSavedAsync() {
        ArgumentCaptor<KafkaProducerRetryJob> captor = ArgumentCaptor.forClass(KafkaProducerRetryJob.class);
        verify(kafkaProducerRetryJobsCollection, timeout(2000)).upsert(anyString(), captor.capture());
        return captor.getValue();
    }

    private static String sha256Hex(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    /** Minimal serializable payload for JSON_SCHEMA path assertions. */
    public static class Payload {
        public int id;
        public String name;

        public Payload() {
        }

        public Payload(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
