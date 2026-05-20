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
class RawSportsBetDetailsProducerTest {

    private static final String TOPIC = KafkaConstant.TOPIC_RAW_SPORTS_BET_DETAILS;
    private static final int AGENT_ID = 77;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private AgentFeatureService agentFeatureService;

    private MeterRegistry meterRegistry;
    private RawSportsBetDetailsProducer producer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        producer = new RawSportsBetDetailsProducer(kafkaTemplate, agentFeatureService, new ObjectMapper(), meterRegistry);
    }

    @Test
    void skipsWhenAgentFeatureDisabled() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(0);

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
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);

        producer.emit(base().vendorBetId(null).build());
        producer.emit(base().roundId(null).build());
        producer.emit(base().requestBody(null).build());

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void sendsPartitionKeyVendorColonRoundId() {
        when(agentFeatureService.getStatus(4242, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base()
                .vendorBetId("bet-1")
                .gaBetId("ga-abc-123")
                .roundId("round-42")
                .vendorPlayerUsername("player-99")
                .agentId(4242)
                .requestBody("{\"a\":1}")
                .build());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), keyCaptor.capture(), valueCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("pinnacle:round-42");
        assertThat(valueCaptor.getValue()).contains("\"idempotencyKey\":\"pinnacle:bet-1:PLACE_BET\"");
        assertThat(valueCaptor.getValue()).contains("\"requestBody\":{\"a\":1}");
        assertThat(valueCaptor.getValue()).contains("\"producerSource\":\"core-engine\"");
        assertThat(valueCaptor.getValue()).contains("\"gaBetId\":\"ga-abc-123\"");
        assertThat(valueCaptor.getValue()).contains("\"vendorPlayerUsername\":\"player-99\"");
        assertThat(valueCaptor.getValue()).contains("\"agentId\":4242");
    }

    @Test
    void sendsWhenEnrichmentFieldsMissing() {
        // gaBetId and vendorPlayerUsername are nullable defensive extras — absent fields
        // should not block the emit (agentId is required for the feature gate and cannot be null).
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base()
                .gaBetId(null)
                .vendorPlayerUsername(null)
                .build());

        verify(kafkaTemplate).send(eq(TOPIC), eq("pinnacle:round-1"), any());
    }

    @Test
    void resettleVersionStampsVersionedIdempotencyKey() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base()
                .vendor("pinnacle")
                .eventFamily("wagering")
                .eventKind(EventKind.RESULT_UPDATE)
                .vendorBetId("W1")
                .roundId("W1")
                .resettleVersion(987654321L)
                .build());

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("pinnacle:W1"), valueCaptor.capture());
        assertThat(valueCaptor.getValue()).contains("\"idempotencyKey\":\"pinnacle:W1:RESULT_UPDATE:v987654321\"");
    }

    @Test
    void nullResettleVersionEmitsUnversionedIdempotencyKey() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base()
                .vendor("pinnacle")
                .eventFamily("wagering")
                .eventKind(EventKind.RESULT_UPDATE)
                .vendorBetId("W1")
                .roundId("W1")
                .build());

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("pinnacle:W1"), valueCaptor.capture());
        assertThat(valueCaptor.getValue()).contains("\"idempotencyKey\":\"pinnacle:W1:RESULT_UPDATE\"");
    }

    @Test
    void incrementsOutcomeCounterAndLatencyTimerOnSuccess() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        producer.emit(base().build());

        // "enqueued" = synchronous handoff to the Kafka producer buffer (wallet-path outcome).
        // "send_ack" = async broker-ack outcome (terminal); "error_send" would replace it on failure.
        double enqueuedCount = meterRegistry.counter("ga.raw_sports_bet_details.emit",
                "vendor", "pinnacle", "event_family", "wagering", "event_kind", "PLACE_BET", "outcome", "enqueued")
                .count();
        double sendAckCount = meterRegistry.counter("ga.raw_sports_bet_details.emit",
                "vendor", "pinnacle", "event_family", "wagering", "event_kind", "PLACE_BET", "outcome", "send_ack")
                .count();
        long latencySamples = meterRegistry.timer("ga.raw_sports_bet_details.emit.latency",
                "vendor", "pinnacle", "event_family", "wagering", "outcome", "enqueued")
                .count();

        assertThat(enqueuedCount).isEqualTo(1.0);
        assertThat(sendAckCount).isEqualTo(1.0);
        assertThat(latencySamples).isEqualTo(1L);
    }

    @Test
    void incrementsSkippedCounterWhenFeatureDisabled() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(0);

        producer.emit(base().build());

        double skippedCount = meterRegistry.counter("ga.raw_sports_bet_details.emit",
                "vendor", "pinnacle", "event_family", "wagering", "event_kind", "PLACE_BET", "outcome", "skipped_feature_disabled")
                .count();
        assertThat(skippedCount).isEqualTo(1.0);
    }

    @Test
    void swallowsKafkaSendFailuresWithoutThrowing() {
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(failed);

        // must not throw — wallet path must not be affected
        producer.emit(base().build());
    }

    @Test
    void reportsErrorSendWhenBrokerFails() {
        // Broker-level failure: sync phase is still "enqueued" (we handed the payload off fine),
        // but the async callback records "error_send" — this is the terminal outcome dashboards
        // should watch.
        when(agentFeatureService.getStatus(AGENT_ID, Features.RAW_SPORTS_BET_DETAILS_EMIT)).thenReturn(1);
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(failed);

        producer.emit(base().build());

        double enqueuedCount = meterRegistry.counter("ga.raw_sports_bet_details.emit",
                "vendor", "pinnacle", "event_family", "wagering", "event_kind", "PLACE_BET", "outcome", "enqueued")
                .count();
        double errorSendCount = meterRegistry.counter("ga.raw_sports_bet_details.emit",
                "vendor", "pinnacle", "event_family", "wagering", "event_kind", "PLACE_BET", "outcome", "error_send")
                .count();
        double sendAckCount = meterRegistry.counter("ga.raw_sports_bet_details.emit",
                "vendor", "pinnacle", "event_family", "wagering", "event_kind", "PLACE_BET", "outcome", "send_ack")
                .count();

        assertThat(enqueuedCount).isEqualTo(1.0);
        assertThat(errorSendCount).isEqualTo(1.0);
        assertThat(sendAckCount).isEqualTo(0.0);
    }

    private BetDetailEmitRequest.BetDetailEmitRequestBuilder base() {
        return BetDetailEmitRequest.builder()
                .vendor("pinnacle")
                .eventFamily("wagering")
                .eventKind(EventKind.PLACE_BET)
                .vendorBetId("bet-1")
                .gaBetId("ga-1")
                .roundId("round-1")
                .vendorPlayerUsername("player-1")
                .agentId(AGENT_ID)
                .requestBody("{}");
    }

    private CompletableFuture<SendResult<String, String>> completed() {
        return CompletableFuture.completedFuture(null);
    }
}
