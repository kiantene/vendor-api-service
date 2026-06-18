package com.nextgen.gameaggregator.data.kafka.betdetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.service.AgentFeatureService;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.enums.Features;
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

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RawBetDetailsProducerTest {

    private static final String TOPIC = KafkaConstant.TOPIC_RAW_BET_DETAILS;
    private static final int AGENT_ID = 77;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private AgentFeatureService agentFeatureService;

    private MeterRegistry meterRegistry;
    private RawBetDetailsProducer producer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        producer = new RawBetDetailsProducer(kafkaTemplate, agentFeatureService, new ObjectMapper(), meterRegistry);
    }

    @Test
    void skipsWhenAgentFeatureDisabled() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(0);

        producer.emit(base().build());

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void skipsWhenAgentIdIsNull() {
        producer.emit(base().agentId(null).build());

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(agentFeatureService, never()).getStatus(any(), any());
    }

    @Test
    void skipsWhenRequiredFieldNull() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);

        producer.emit(base().vendorBetId(null).build());
        producer.emit(base().requestBody(null).build());
        producer.emit(base().bodyFormat(null).build());

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void skipsWhenBodyFormatInvalid() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);

        producer.emit(base().bodyFormat("xml").build());

        verify(kafkaTemplate, never()).send(any(), any(), any());
        double invalidCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "5",
                "body_format", "invalid", "outcome", "skipped_invalid_body_format")
                .count();
        assertThat(invalidCount).isEqualTo(1.0);
    }

    @Test
    void emitsWhenRoundIdIsNullAndPartitionsByVendorBetId() {
        // EVOLIVE rollback path: roundId is null on the wire — we must still emit
        // and partition off vendorBetId so retries land on the same partition.
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base().roundId(null).vendorBetId("BET-X").build());

        verify(kafkaTemplate).send(eq(TOPIC), eq("evolive:BET-X"), any());
    }

    @Test
    void sendsPartitionKeyVendorColonRoundId() {
        when(agentFeatureService.getStatus(4242, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base()
                .vendor("evolive")
                .vendorBetId("bet-1")
                .gaBetId("ga-abc-123")
                .roundId("round-42")
                .vendorPlayerUsername("player-99")
                .agentId(4242)
                .gameCategoryId(5)
                .bodyFormat("json")
                .requestBody("{\"a\":1}")
                .build());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), keyCaptor.capture(), valueCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("evolive:round-42");
        String payload = valueCaptor.getValue();
        // Plain string requestBody (escaped JSON), NOT @JsonRawValue embedded.
        assertThat(payload).contains("\"requestBody\":\"{\\\"a\\\":1}\"");
        assertThat(payload).contains("\"idempotencyKey\":\"evolive:bet-1:PLACE_BET\"");
        assertThat(payload).contains("\"producerSource\":\"core-engine\"");
        assertThat(payload).contains("\"gaBetId\":\"ga-abc-123\"");
        assertThat(payload).contains("\"vendorPlayerUsername\":\"player-99\"");
        assertThat(payload).contains("\"agentId\":4242");
        assertThat(payload).contains("\"gameCategoryId\":5");
        assertThat(payload).contains("\"bodyFormat\":\"json\"");
    }

    @Test
    void preservesFormBodyAsEscapedString() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base()
                .vendor("yeebet")
                .bodyFormat("form")
                .requestBody("a=1&b=2&c=hello%20world")
                .build());

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), any(), valueCaptor.capture());
        String payload = valueCaptor.getValue();
        assertThat(payload).contains("\"bodyFormat\":\"form\"");
        assertThat(payload).contains("\"requestBody\":\"a=1&b=2&c=hello%20world\"");
    }

    @Test
    void emitsWhenGameCategoryIdIsNullAndTagsUnknown() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base().gameCategoryId(null).build());

        verify(kafkaTemplate).send(eq(TOPIC), any(), any());
        double enqueuedCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "unknown",
                "body_format", "json", "outcome", "enqueued")
                .count();
        assertThat(enqueuedCount).isEqualTo(1.0);
    }

    @Test
    void incrementsOutcomeCounterAndLatencyTimerOnSuccess() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base().build());

        double enqueuedCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "5",
                "body_format", "json", "outcome", "enqueued")
                .count();
        double sendAckCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "5",
                "body_format", "json", "outcome", "send_ack")
                .count();
        long latencySamples = meterRegistry.timer("ga.raw_bet_details.emit.latency",
                "vendor", "evolive", "body_format", "json", "outcome", "enqueued")
                .count();

        assertThat(enqueuedCount).isEqualTo(1.0);
        assertThat(sendAckCount).isEqualTo(1.0);
        assertThat(latencySamples).isEqualTo(1L);
    }

    @Test
    void incrementsSkippedCounterWhenFeatureDisabled() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(0);

        producer.emit(base().build());

        double skippedCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "5",
                "body_format", "json", "outcome", "skipped_feature_disabled")
                .count();
        assertThat(skippedCount).isEqualTo(1.0);
    }

    @Test
    void swallowsKafkaSendFailuresWithoutThrowing() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(failed);

        producer.emit(base().build());
    }

    @Test
    void reportsErrorSendWhenBrokerFails() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_BET_DETAILS_EMIT)).thenReturn(1);
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(failed);

        producer.emit(base().build());

        double enqueuedCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "5",
                "body_format", "json", "outcome", "enqueued")
                .count();
        double errorSendCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "5",
                "body_format", "json", "outcome", "error_send")
                .count();
        double sendAckCount = meterRegistry.counter("ga.raw_bet_details.emit",
                "vendor", "evolive", "event_kind", "PLACE_BET", "game_category_id", "5",
                "body_format", "json", "outcome", "send_ack")
                .count();

        assertThat(enqueuedCount).isEqualTo(1.0);
        assertThat(errorSendCount).isEqualTo(1.0);
        assertThat(sendAckCount).isEqualTo(0.0);
    }

    private BetDetailEmitRequest.BetDetailEmitRequestBuilder base() {
        return BetDetailEmitRequest.builder()
                .vendor("evolive")
                .eventKind(EventKind.PLACE_BET)
                .vendorBetId("bet-1")
                .gaBetId("ga-1")
                .roundId("round-1")
                .vendorPlayerUsername("player-1")
                .agentId(AGENT_ID)
                .gameCategoryId(5)
                .bodyFormat("json")
                .requestBody("{}");
    }

    private CompletableFuture<SendResult<String, String>> completed() {
        return CompletableFuture.completedFuture(null);
    }
}
