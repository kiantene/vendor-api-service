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
import java.util.Set;

@Slf4j
@Component
public class RawBetDetailsProducer {

    private static final String PRODUCER_SOURCE = "core-engine";

    private static final String EMIT_COUNTER = "ga.raw_bet_details.emit";
    private static final String EMIT_LATENCY = "ga.raw_bet_details.emit.latency";

    private static final String BODY_FORMAT_JSON = "json";
    private static final String BODY_FORMAT_FORM = "form";
    private static final Set<String> ALLOWED_BODY_FORMATS = Set.of(BODY_FORMAT_JSON, BODY_FORMAT_FORM);

    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final AgentFeatureService agentFeatureService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RawBetDetailsProducer(KafkaTemplate<String, String> stringKafkaTemplate,
                                 AgentFeatureService agentFeatureService,
                                 ObjectMapper objectMapper,
                                 MeterRegistry meterRegistry) {
        this.stringKafkaTemplate = stringKafkaTemplate;
        this.agentFeatureService = agentFeatureService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Emit a raw livecasino bet-detail event. Executed on the {@code rawBetDetailsExecutor}
     * (see {@code AsyncConfig}) — never on the calling (betting-request) thread. Under
     * back-pressure the task is dropped by {@link DropAndCountRejectionPolicy}; the caller
     * is never blocked and never sees an exception.
     *
     * <p>NOTE: this is a Spring-AOP-proxied {@code @Async} method. Call it from <em>outside</em>
     * the bean only — a {@code this.emit(...)} self-invocation would bypass the proxy and run
     * inline on the caller thread.
     */
    @Async("rawBetDetailsExecutor")
    public void emit(BetDetailEmitRequest request) {
        long startNanos = System.nanoTime();
        // Runs on the rawBetDetailsExecutor thread. Outcome "enqueued" means gate checks passed
        // and the payload was handed to the Kafka producer buffer. Terminal broker outcome lands
        // asynchronously in whenComplete as send_ack / error_send.
        String outcome = "enqueued";
        // Tag value for the bodyFormat dimension. We never tag with the raw input value (that
        // would let a typo blow up cardinality) — invalid/missing collapse to "invalid"/"unknown".
        String bodyFormatTag = safeBodyFormatTag(request.getBodyFormat());
        try {
            if (request.getAgentId() == null) {
                outcome = "skipped_missing_agent_id";
                log.warn("Skipping raw bet detail emit: agentId is null vendor={} eventKind={} vendorBetId={} roundId={}",
                        request.getVendor(), request.getEventKind(), request.getVendorBetId(), request.getRoundId());
                return;
            }
            Integer featureStatus = agentFeatureService.getStatus(request.getAgentId(), Features.RAW_BET_DETAILS_EMIT);
            if (featureStatus == null || featureStatus != 1) {
                outcome = "skipped_feature_disabled";
                return;
            }
            if (request.getBodyFormat() == null) {
                outcome = "skipped_missing_fields";
                return;
            }
            if (!ALLOWED_BODY_FORMATS.contains(request.getBodyFormat())) {
                outcome = "skipped_invalid_body_format";
                log.warn("Skipping raw bet detail emit: invalid bodyFormat='{}' vendor={} vendorBetId={}",
                        request.getBodyFormat(), request.getVendor(), request.getVendorBetId());
                return;
            }
            if (request.getVendorBetId() == null || request.getRequestBody() == null) {
                outcome = "skipped_missing_fields";
                return;
            }

            RawBetDetailEvent event = RawBetDetailEvent.builder()
                    .idempotencyKey(IdempotencyKey.of(request.getVendor(), request.getVendorBetId(), request.getEventKind()))
                    .vendor(request.getVendor())
                    .eventKind(request.getEventKind())
                    .vendorBetId(request.getVendorBetId())
                    .gaBetId(request.getGaBetId())
                    .roundId(request.getRoundId())
                    .vendorPlayerUsername(request.getVendorPlayerUsername())
                    .agentId(request.getAgentId())
                    .gameCategoryId(request.getGameCategoryId())
                    .producerSource(PRODUCER_SOURCE)
                    .receivedAt(System.currentTimeMillis())
                    .bodyFormat(request.getBodyFormat())
                    .requestBody(request.getRequestBody())
                    .build();

            // Partition key: prefer roundId, fall back to vendorBetId — required because EVOLIVE
            // rollback (and possibly others) legitimately have no roundId on the wire.
            String partitionKeySuffix = request.getRoundId() != null ? request.getRoundId() : request.getVendorBetId();
            String partitionKey = request.getVendor() + ":" + partitionKeySuffix;
            String payload;
            try {
                payload = objectMapper.writeValueAsString(event);
            } catch (Exception ex) {
                outcome = "error_build";
                log.error("Error building raw bet detail event vendor={} eventKind={} vendorBetId={} roundId={}: {}",
                        request.getVendor(), request.getEventKind(), request.getVendorBetId(), request.getRoundId(), ex.getMessage());
                return;
            }

            stringKafkaTemplate.send(KafkaConstant.TOPIC_RAW_BET_DETAILS, partitionKey, payload).whenComplete((result, throwable) -> {
                String sendOutcome = throwable == null ? "send_ack" : "error_send";
                incrementOutcome(request, sendOutcome, bodyFormatTag);
                if (throwable != null) {
                    log.error("Failed to emit raw bet detail event vendor={} eventKind={} vendorBetId={} roundId={}",
                            request.getVendor(), request.getEventKind(), request.getVendorBetId(), request.getRoundId(), throwable);
                }
            });
        } catch (Exception ex) {
            outcome = "error_build";
            log.error("Unexpected error in raw bet detail emit vendor={} eventKind={} vendorBetId={} roundId={}: {}",
                    request.getVendor(), request.getEventKind(), request.getVendorBetId(), request.getRoundId(), ex.getMessage());
        } finally {
            incrementOutcome(request, outcome, bodyFormatTag);
            Timer.builder(EMIT_LATENCY)
                    .description("Raw bet detail emit processing latency (runs async on rawBetDetailsExecutor, not on the caller thread)")
                    .tag("vendor", safeTag(request.getVendor()))
                    .tag("body_format", bodyFormatTag)
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(Duration.ofNanos(System.nanoTime() - startNanos));
        }
    }

    private void incrementOutcome(BetDetailEmitRequest request, String outcome, String bodyFormatTag) {
        Counter.builder(EMIT_COUNTER)
                .description("Raw bet detail emit attempts, tagged by terminal outcome")
                .tag("vendor", safeTag(request.getVendor()))
                .tag("event_kind", request.getEventKind() == null ? "unknown" : request.getEventKind().name())
                .tag("game_category_id", request.getGameCategoryId() == null ? "unknown" : request.getGameCategoryId().toString())
                .tag("body_format", bodyFormatTag)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    private static String safeTag(String value) {
        return value == null ? "unknown" : value;
    }

    private static String safeBodyFormatTag(String bodyFormat) {
        if (bodyFormat == null) {
            return "unknown";
        }
        return ALLOWED_BODY_FORMATS.contains(bodyFormat) ? bodyFormat : "invalid";
    }
}
