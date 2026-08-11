package com.nextgen.gameaggregator.core.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.service.KafkaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LogContextServiceTest {

    private KafkaService kafkaService;
    private LogContextService logContextService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        kafkaService = mock(KafkaService.class);
        logContextService = new LogContextService(kafkaService);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(LogContextService.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(LogContextService.class)).detachAppender(logAppender);
        logContextService.shutdownApiRequestLogPublisher();
        LogContextHolder.clear();
    }

    @Test
    void logApiRequestBuildsOnCallerThreadAndPublishesOffIt() throws Exception {
        LogContext logContext = new LogContext();
        logContext.put(HttpRequestLog.class.getSimpleName(), new HttpRequestLog());

        Thread callerThread = Thread.currentThread();
        CountDownLatch published = new CountDownLatch(1);
        AtomicReference<Thread> publishThread = new AtomicReference<>();
        doAnswer(invocation -> {
            publishThread.set(Thread.currentThread());
            published.countDown();
            return null;
        }).when(kafkaService).produceApiRequestLog(any(ApiRequestLog.class));

        logContextService.logApiRequest(logContext, "response");

        // build ran synchronously on the caller thread — it consumes the HttpRequestLog entry
        assertThat(logContext.exists(HttpRequestLog.class.getSimpleName())).isFalse();
        // publish ran on the dedicated executor, never the caller thread
        assertThat(published.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(publishThread.get()).isNotSameAs(callerThread);
        assertThat(publishThread.get().getName()).startsWith("api-request-log-publisher-");
    }

    @Test
    void logApiRequestDoesNotPublishWhenThereIsNoHttpRequestLog() {
        LogContext logContext = new LogContext();

        logContextService.logApiRequest(logContext, "response");

        verify(kafkaService, after(300).never()).produceApiRequestLog(any());
    }

    @Test
    void logApiRequestFallsBackToLocalLogWhenPublisherCannotAcceptTheTask() {
        // once the publisher is shut down, execute() rejects new tasks — the rejection handler
        // must dump the payload to the local log instead of throwing or losing it
        logContextService.shutdownApiRequestLogPublisher();

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setRoundId("round-x");
        LogContext logContext = new LogContext();
        logContext.put(HttpRequestLog.class.getSimpleName(), httpRequestLog);

        logContextService.logApiRequest(logContext, "response");

        verify(kafkaService, after(300).never()).produceApiRequestLog(any());
        assertThat(localLogContains("round-x")).isTrue();
    }

    @Test
    void logApiRequestDumpsPayloadLocallyWhenPublishThrowsUnexpectedly() throws Exception {
        // produceApiRequestLog handles its own failures; simulate an unexpected escape so the
        // async task's outermost catch fires — it must dump the payload, not just log the roundId
        doThrow(new RuntimeException("boom")).when(kafkaService).produceApiRequestLog(any());

        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setRoundId("round-boom");
        LogContext logContext = new LogContext();
        logContext.put(HttpRequestLog.class.getSimpleName(), httpRequestLog);

        logContextService.logApiRequest(logContext, "response");

        // the JSON payload form (only logLocally emits it) must appear — the plain error log
        // uses roundId=[...] instead, so this fails if run() drops the logLocally fallback
        assertThat(localLogContainsEventually("\"roundId\":\"round-boom\"", 5000)).isTrue();
    }

    private boolean localLogContains(String needle) {
        return logAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(needle));
    }

    private boolean localLogContainsEventually(String needle, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (localLogContains(needle)) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
