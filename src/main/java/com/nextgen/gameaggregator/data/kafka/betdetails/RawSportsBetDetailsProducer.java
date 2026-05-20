package com.nextgen.gameaggregator.data.kafka.betdetails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.service.AgentFeatureService;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.enums.Features;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class RawSportsBetDetailsProducer {

    private static final String PRODUCER_SOURCE = "core-engine";

    // Metric names — AP-0016 NFR-03 (Stage-1 producer observability).
    private static final String EMIT_COUNTER = "ga.raw_sports_bet_details.emit";
    private static final String EMIT_LATENCY = "ga.raw_sports_bet_details.emit.latency";

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final AgentFeatureService agentFeatureService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RawSportsBetDetailsProducer(KafkaTemplate<String, String> stringKafkaTemplate,
                                       AgentFeatureService agentFeatureService,
                                       ObjectMapper objectMapper,
                                       MeterRegistry meterRegistry) {
        this.stringKafkaTemplate = stringKafkaTemplate;
        this.agentFeatureService = agentFeatureService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Emit a raw sports bet-detail event. Executed on the {@code rawSportsBetDetailsExecutor}
     * (see {@code AsyncConfig}) — never on the calling (betting-request) thread. Under
     * back-pressure the task is dropped by {@link DropAndCountRejectionPolicy}; the caller
     * is never blocked and never sees an exception.
     *
     * <p>NOTE: this is a Spring-AOP-proxied {@code @Async} method. Call it from <em>outside</em>
     * the bean only — a {@code this.emit(...)} self-invocation would bypass the proxy and run
     * inline on the caller thread.
     */
    @Async("rawSportsBetDetailsExecutor")
    public void emit(BetDetailEmitRequest request) {
        long startNanos = System.nanoTime();
        // Runs on the rawSportsBetDetailsExecutor thread. Outcome "enqueued" means gate checks
        // passed and the payload was handed to the Kafka producer buffer. Terminal broker outcome
        // lands asynchronously in the whenComplete callback below as send_ack / error_send.
        String outcome = "enqueued";
        try {
            if (request.getAgentId() == null) {
                outcome = "skipped_missing_agent_id";
                log.warn("Skipping raw sports bet detail emit: agentId is null vendor={} eventFamily={} vendorBetId={} roundId={}",
                        request.getVendor(), request.getEventFamily(), request.getVendorBetId(), request.getRoundId());
                return;
            }
            Integer featureStatus = agentFeatureService.getStatus(request.getAgentId(), Features.RAW_SPORTS_BET_DETAILS_EMIT);
            if (featureStatus == null || featureStatus != 1) {
                outcome = "skipped_feature_disabled";
                return;
            }
            if (request.getVendorBetId() == null || request.getRoundId() == null || request.getRequestBody() == null) {
                outcome = "skipped_missing_fields";
                return;
            }

            RawSportsBetDetailEvent event = RawSportsBetDetailEvent.builder()
                    .idempotencyKey(IdempotencyKey.of(request.getVendor(), request.getVendorBetId(), request.getEventKind(), request.getResettleVersion()))
                    .vendor(request.getVendor())
                    .eventKind(request.getEventKind())
                    .vendorBetId(request.getVendorBetId())
                    .gaBetId(request.getGaBetId())
                    .roundId(request.getRoundId())
                    .vendorPlayerUsername(request.getVendorPlayerUsername())
                    .agentId(request.getAgentId())
                    .producerSource(PRODUCER_SOURCE)
                    .receivedAt(System.currentTimeMillis())
                    .requestBody(request.getRequestBody())
                    .build();

            String partitionKey = request.getVendor() + ":" + request.getRoundId();
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (Exception ex) {
                outcome = "error_build";
                log.error("Error building raw sports bet detail event vendor={} eventKind={} vendorBetId={} roundId={}: {}",
                        request.getVendor(), request.getEventKind(), request.getVendorBetId(), request.getRoundId(), ex.getMessage());
                return;
            }

            stringKafkaTemplate.send(KafkaConstant.TOPIC_RAW_SPORTS_BET_DETAILS, partitionKey, payload).whenComplete((result, throwable) -> {
                String sendOutcome = throwable == null ? "send_ack" : "error_send";
                incrementOutcome(request, sendOutcome);
                if (throwable != null) {
                    log.error("Failed to emit raw sports bet detail event vendor={} eventKind={} vendorBetId={} roundId={}",
                            request.getVendor(), request.getEventKind(), request.getVendorBetId(), request.getRoundId(), throwable);
                }
            });
        } catch (Exception ex) {
            outcome = "error_build";
            log.error("Unexpected error in raw sports bet detail emit vendor={} eventKind={} vendorBetId={} roundId={}: {}",
                    request.getVendor(), request.getEventKind(), request.getVendorBetId(), request.getRoundId(), ex.getMessage());
        } finally {
            incrementOutcome(request, outcome);
            Timer.builder(EMIT_LATENCY)
                    .description("Raw sports bet detail emit processing latency (runs async on rawSportsBetDetailsExecutor, not on the caller thread)")
                    .tag("vendor", safeTag(request.getVendor()))
                    .tag("event_family", safeTag(request.getEventFamily()))
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(Duration.ofNanos(System.nanoTime() - startNanos));
        }
    }

    private void incrementOutcome(BetDetailEmitRequest request, String outcome) {
        Counter.builder(EMIT_COUNTER)
                .description("Raw sports bet detail emit attempts, tagged by terminal outcome")
                .tag("vendor", safeTag(request.getVendor()))
                .tag("event_family", safeTag(request.getEventFamily()))
                .tag("event_kind", request.getEventKind() == null ? "unknown" : request.getEventKind().name())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    private static String safeTag(String value) {
        return value == null ? "unknown" : value;
    }
}
