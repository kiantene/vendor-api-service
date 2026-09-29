package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.BetHistoryV3;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.service.kafka.KafkaDlqService;
import com.nextgen.gameaggregator.service.kafka.KafkaSerializerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * ONEAPI-1349 — the Kafka fallback dedup key must separate every event of one bet.
 *
 * <p>A cancel after a settle reuses the settle's {@code betId} and {@code roundId}, so a key of
 * {@code vendorId::id::roundId} upserts the cancel payload over the still-unreplayed settle payload
 * and the settle row is lost. Each test below pins one axis of the key.
 */
@ExtendWith(MockitoExtension.class)
class KafkaServiceBetHistoryDedupKeyTest {

    private static final Integer VENDOR_ID = 1;           // Pragmatic Play
    private static final String BET_ID = "0199c7a1-bet-id";
    private static final String ROUND_ID = "round-77";

    @Mock
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Mock
    private KafkaTemplate<String, Object> jsonSchemaKafkaTemplate;
    @Mock
    private KafkaTemplate<String, Object> apiRequestLogKafkaTemplate;
    @Mock
    private CurrencyConversionService currencyConversionService;
    @Mock
    private WarehouseBetHistoryService warehouseBetHistoryService;
    @Mock
    private AgentPlayerService agentPlayerService;
    @Mock
    private VendorPlayerService vendorPlayerService;
    @Mock
    private S3BetService s3BetService;
    @Mock
    private KafkaDlqService kafkaDlqService;

    private KafkaService kafkaService;

    @BeforeEach
    void setUp() {
        kafkaService = new KafkaService(stringKafkaTemplate, jsonSchemaKafkaTemplate, apiRequestLogKafkaTemplate,
                currencyConversionService, warehouseBetHistoryService, agentPlayerService, vendorPlayerService,
                s3BetService, kafkaDlqService);
    }

    /** Same bet, same round — only status and resettleNum differ. This is the reported bug. */
    @Test
    void settleThenCancelOfSameBet_produceDifferentDedupKeys() {
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.SETTLED.code, 0));
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.CANCELLED.code, 1));

        List<String> keys = capturedDedupKeys(2);
        assertThat(keys.get(0)).isNotEqualTo(keys.get(1));
    }

    /**
     * Same bet, same resettleNum — only status differs. REFUNDED is the one status that never bumps
     * resettleNum, so keying on resettleNum alone would collapse this pair onto the settle's job.
     */
    @Test
    void settleThenRefundAtSameResettleNum_produceDifferentDedupKeys() {
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.SETTLED.code, 0));
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.REFUNDED.code, 0));

        List<String> keys = capturedDedupKeys(2);
        assertThat(keys.get(0)).isNotEqualTo(keys.get(1));
    }

    /** Same bet, same status — only resettleNum differs (a second cancel after a re-settle). */
    @Test
    void successiveResettlesOfSameBet_produceDifferentDedupKeys() {
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.CANCELLED.code, 1));
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.CANCELLED.code, 2));

        List<String> keys = capturedDedupKeys(2);
        assertThat(keys.get(0)).isNotEqualTo(keys.get(1));
    }

    /** A vendor resend of the SAME event must still collapse to one buffered job, not two. */
    @Test
    void resendOfSameEvent_producesSameDedupKey() {
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.SETTLED.code, 0));
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.SETTLED.code, 0));

        List<String> keys = capturedDedupKeys(2);
        assertThat(keys.get(0)).isEqualTo(keys.get(1));
    }

    /** A null resettleNum normalises to 0 so the key stays usable rather than falling back to a hash. */
    @Test
    void nullResettleNum_isNormalisedToZero() {
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.SETTLED.code, null));
        kafkaService.produceBetHistoryV3(betHistory(BetStatus.SETTLED.code, 0));

        List<String> keys = capturedDedupKeys(2);
        assertThat(keys.get(0)).isEqualTo(keys.get(1));
        assertThat(keys.get(0)).isEqualTo(VENDOR_ID + "::" + BET_ID + "::" + ROUND_ID + "::"
                + BetStatus.SETTLED.code + "::0");
    }

    private BetHistoryV3 betHistory(Integer status, Integer resettleNum) {
        BetHistoryV3 betHistoryV3 = new BetHistoryV3();
        betHistoryV3.setVendorId(VENDOR_ID);
        betHistoryV3.setId(BET_ID);
        betHistoryV3.setRoundId(ROUND_ID);
        betHistoryV3.setStatus(status);
        betHistoryV3.setResettleNum(resettleNum);
        return betHistoryV3;
    }

    private List<String> capturedDedupKeys(int expectedSends) {
        ArgumentCaptor<String> dedupKey = ArgumentCaptor.forClass(String.class);
        verify(kafkaDlqService, times(expectedSends)).sendWithFallback(
                eq(KafkaConstant.TOPIC_BET_HISTORY_V4), eq(null), dedupKey.capture(),
                any(), eq(KafkaSerializerType.STRING));
        return dedupKey.getAllValues();
    }
}
