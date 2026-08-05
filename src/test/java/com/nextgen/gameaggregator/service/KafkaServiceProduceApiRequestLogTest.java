package com.nextgen.gameaggregator.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nextgen.gameaggregator.data.kafka.constant.KafkaConstant;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaServiceProduceApiRequestLogTest {

    private static final String TOPIC = KafkaConstant.TOPIC_API_REQUEST_LOG;
    private static final String DELIVERY_FAILED_MARKER = "ApiRequestLog delivery failed";

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

    private KafkaService kafkaService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        kafkaService = new KafkaService(stringKafkaTemplate, jsonSchemaKafkaTemplate, apiRequestLogKafkaTemplate,
                currencyConversionService, warehouseBetHistoryService, agentPlayerService, vendorPlayerService,
                s3BetService);
        ReflectionTestUtils.setField(kafkaService, "logToKafka", true);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(KafkaService.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(KafkaService.class)).detachAppender(logAppender);
    }

    @Test
    void sendsViaDedicatedTemplateKeyedByUsername() {
        ApiRequestLog apiRequestLog = apiRequestLog();
        when(apiRequestLogKafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        kafkaService.produceApiRequestLog(apiRequestLog);

        verify(apiRequestLogKafkaTemplate).send(TOPIC, "player-1", apiRequestLog);
        verify(jsonSchemaKafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void successProducesNoDeliveryFailedLines() {
        when(apiRequestLogKafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(completed());

        kafkaService.produceApiRequestLog(apiRequestLog());

        assertThat(deliveryFailedCount()).isZero();
    }

    @Test
    void asyncFailureLogsDeliveryFailedWithStackTraceAndPayload() {
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("schema subject not found"));
        when(apiRequestLogKafkaTemplate.send(eq(TOPIC), any(), any())).thenReturn(failed);

        kafkaService.produceApiRequestLog(apiRequestLog());

        assertThat(deliveryFailedCount()).isEqualTo(1);
        assertThat(deliveryFailedHasStackTrace()).isTrue();
        assertThat(payloadLogged()).isTrue();
    }

    @Test
    void syncThrowLogsDeliveryFailedWithStackTraceAndPayload() {
        when(apiRequestLogKafkaTemplate.send(eq(TOPIC), any(), any()))
                .thenThrow(new RuntimeException("max.block.ms exceeded"));

        kafkaService.produceApiRequestLog(apiRequestLog());

        assertThat(deliveryFailedCount()).isEqualTo(1);
        assertThat(deliveryFailedHasStackTrace()).isTrue();
        assertThat(payloadLogged()).isTrue();
    }

    @Test
    void logToKafkaDisabledLogsLocallyAndNeverTouchesTemplate() {
        ReflectionTestUtils.setField(kafkaService, "logToKafka", false);

        kafkaService.produceApiRequestLog(apiRequestLog());

        verify(apiRequestLogKafkaTemplate, never()).send(any(), any(), any());
        assertThat(deliveryFailedCount()).isZero();
        assertThat(payloadLogged()).isTrue();
    }

    private ApiRequestLog apiRequestLog() {
        ApiRequestLog apiRequestLog = new ApiRequestLog(new HttpRequestLog());
        apiRequestLog.setUsername("player-1");
        apiRequestLog.setRoundId("round-1");
        return apiRequestLog;
    }

    private long deliveryFailedCount() {
        return logAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains(DELIVERY_FAILED_MARKER))
                .count();
    }

    private boolean deliveryFailedHasStackTrace() {
        // the third arg of the delivery-failed log is StackTraceUtils.getStackTraceAsString(...),
        // so a real stack frame ("\tat ") must appear in the formatted message
        return logAppender.list.stream()
                .filter(event -> event.getFormattedMessage().contains(DELIVERY_FAILED_MARKER))
                .anyMatch(event -> event.getFormattedMessage().contains("\tat "));
    }

    private boolean payloadLogged() {
        return logAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("\"roundId\":\"round-1\""));
    }

    private CompletableFuture<SendResult<String, Object>> completed() {
        return CompletableFuture.completedFuture(null);
    }
}
