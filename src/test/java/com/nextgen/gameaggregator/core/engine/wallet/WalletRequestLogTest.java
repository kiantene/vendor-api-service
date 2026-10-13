package com.nextgen.gameaggregator.core.engine.wallet;

import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.logging.ApiRequestLog;
import com.nextgen.gameaggregator.service.KafkaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * OVI-3176. Two things were wrong on the debit/credit engine:
 * <ul>
 *   <li>a vendor-owned log (JDB) never had the operator call recorded on it, because it does not
 *       pass through LogContextService.logApiRequest;</li>
 *   <li>a vendor WITHOUT its own log got no api_request_log row at all, because unlike the five
 *       bet/result-family wrappers the debit/credit wrappers created and registered nothing.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class WalletRequestLogTest {

    private static final String REQUEST_TYPE = "WalletDebitAction";
    /** logApiRequest publishes on a thread pool; wait for it rather than racing it. */
    private static final long PUBLISH_TIMEOUT_MS = 5_000L;
    /** Long enough that a queued publish would have surfaced. */
    private static final long PUBLISH_SETTLE_MS = 300L;

    @Mock private KafkaService kafkaService;

    private static LogContext contextWithOperatorCall() {
        LogContext logContext = new LogContext();
        logContext.setUrl("/api/v1/jdb/action/1681359408");
        logContext.setApiUrl("https://operator.example/api/v1/wallet/bet/debit");
        logContext.setApiBody(Map.of("username", "tm63tllpc"));
        logContext.setApiResponse("{\"status\":\"SC_OK\"}");
        logContext.setApiStart(1_700_000_000_000L);
        logContext.setApiEnd(1_700_000_000_250L);
        return logContext;
    }

    @Nested
    @DisplayName("framework-owned: a vendor that declares nothing")
    class FrameworkOwned {

        @Test
        @DisplayName("begin creates a log stamped with the request type")
        void beginCreatesALog() {
            HttpRequestLog log = WalletRequestLog.begin(contextWithOperatorCall(), REQUEST_TYPE);

            assertNotNull(log, "a vendor with no log of its own must still get one");
            assertEquals(REQUEST_TYPE, log.getRequestType());
        }

        @Test
        @DisplayName("end() registers the log — the gate that makes logApiRequest publish it")
        void endRegistersTheLogForPublishing() {
            LogContext logContext = contextWithOperatorCall();
            HttpRequestLog log = WalletRequestLog.begin(logContext, REQUEST_TYPE);

            WalletRequestLog.end(logContext, log);

            assertTrue(logContext.exists(HttpRequestLog.class.getSimpleName()),
                    "a framework-owned log must be registered, or nothing ever publishes it");
            assertSame(log, logContext.get(HttpRequestLog.class.getSimpleName()));
        }

        @Test
        @DisplayName("the row is published, and carries the operator call — the gap this closes")
        void rowIsPublishedWithOperatorFields() {
            LogContext logContext = contextWithOperatorCall();
            HttpRequestLog log = WalletRequestLog.begin(logContext, REQUEST_TYPE);

            WalletRequestLog.end(logContext, log);
            new LogContextService(kafkaService).logApiRequest(logContext, "{}");

            // logApiRequest hands the row to a publisher thread pool, so the interaction cannot be
            // verified instantaneously — wait for it instead of racing it.
            ArgumentCaptor<ApiRequestLog> published = ArgumentCaptor.forClass(ApiRequestLog.class);
            verify(kafkaService, timeout(PUBLISH_TIMEOUT_MS)).produceApiRequestLog(published.capture());
            assertEquals("https://operator.example/api/v1/wallet/bet/debit",
                    published.getValue().getOperatorUrl());
        }
    }

    @Nested
    @DisplayName("vendor-owned: a vendor that publishes its own log, e.g. JDB")
    class VendorOwned {

        @Test
        @DisplayName("begin hands back the vendor's own log, not a framework one")
        void beginReusesTheVendorLog() {
            LogContext logContext = contextWithOperatorCall();
            HttpRequestLog vendorLog = new HttpRequestLog();
            // JDB sets the DECRYPTED payload here; a framework log would carry the encrypted body.
            vendorLog.setRequestBody("{\"action\":9,\"uid\":\"tm63tllpc\"}");
            WalletRequestLog.declareVendorOwned(logContext, vendorLog);

            HttpRequestLog log = WalletRequestLog.begin(logContext, REQUEST_TYPE);

            assertSame(vendorLog, log);
            assertEquals("{\"action\":9,\"uid\":\"tm63tllpc\"}", log.getRequestBody());
        }

        @Test
        @DisplayName("end records the operator call on it")
        void endRecordsOperatorCall() {
            LogContext logContext = contextWithOperatorCall();
            HttpRequestLog vendorLog = new HttpRequestLog();
            WalletRequestLog.declareVendorOwned(logContext, vendorLog);

            WalletRequestLog.end(logContext, WalletRequestLog.begin(logContext, REQUEST_TYPE));

            assertEquals("https://operator.example/api/v1/wallet/bet/debit", vendorLog.getOperatorEndPoints());
            assertEquals("{\"status\":\"SC_OK\"}", vendorLog.getOperatorResponse());
            assertEquals(1_700_000_000_000L, vendorLog.getOperatorStart());
            assertEquals(1_700_000_000_250L, vendorLog.getOperatorEnd());
            assertTrue(vendorLog.getOperatorData().contains("tm63tllpc"));
        }

        @Test
        @DisplayName("and is NOT published by the framework — one row per request")
        void isNotPublishedByTheFramework() {
            LogContext logContext = contextWithOperatorCall();
            WalletRequestLog.declareVendorOwned(logContext, new HttpRequestLog());

            WalletRequestLog.end(logContext, WalletRequestLog.begin(logContext, REQUEST_TYPE));
            new LogContextService(kafkaService).logApiRequest(logContext, "{}");

            // Assert the gate itself, not just the absence of a call: logApiRequest publishes on a
            // thread pool, so "no interaction yet" would pass even if a publish had been queued.
            assertFalse(logContext.exists(HttpRequestLog.class.getSimpleName()),
                    "a vendor-owned log must not be registered under the framework's publish key");
            verify(kafkaService, after(PUBLISH_SETTLE_MS).never()).produceApiRequestLog(any());
        }

        @Test
        @DisplayName("a failed operator call still records url and request body")
        void recordsFailedCall() {
            LogContext logContext = new LogContext();
            // onBeforeSend ran; onResponse never did.
            logContext.setApiUrl("https://operator.example/api/v1/wallet/bet/debit");
            logContext.setApiBody(Map.of("username", "tm63tllpc"));
            HttpRequestLog vendorLog = new HttpRequestLog();
            WalletRequestLog.declareVendorOwned(logContext, vendorLog);

            WalletRequestLog.end(logContext, WalletRequestLog.begin(logContext, REQUEST_TYPE));

            assertEquals("https://operator.example/api/v1/wallet/bet/debit", vendorLog.getOperatorEndPoints());
            assertTrue(vendorLog.getOperatorData().contains("tm63tllpc"));
            assertNull(vendorLog.getOperatorResponse(), "no response arrived, so none should be invented");
        }

        @Test
        @DisplayName("operatorData is a JSON string, not a nested object — GA-14989 mapping trap")
        void operatorDataIsAString() {
            LogContext logContext = contextWithOperatorCall();
            HttpRequestLog vendorLog = new HttpRequestLog();
            WalletRequestLog.declareVendorOwned(logContext, vendorLog);

            WalletRequestLog.end(logContext, WalletRequestLog.begin(logContext, REQUEST_TYPE));

            String data = vendorLog.getOperatorData();
            assertTrue(data.startsWith("{") && data.endsWith("}"), "expected serialised JSON, got: " + data);
        }

        @Test
        @DisplayName("legacy actions keep the operator values they set directly")
        void writeOnceLeavesLegacyValues() {
            LogContext logContext = contextWithOperatorCall();
            HttpRequestLog vendorLog = new HttpRequestLog();
            // What WalletBetAction / WalletBalanceAction set on this same object for actions 4/6/8.
            vendorLog.setOperatorEndPoints("https://operator.example/api/v1/wallet/bet");
            vendorLog.setOperatorData("legacy-request");
            vendorLog.setOperatorResponse("legacy-response");
            WalletRequestLog.declareVendorOwned(logContext, vendorLog);

            WalletRequestLog.end(logContext, WalletRequestLog.begin(logContext, REQUEST_TYPE));

            assertEquals("https://operator.example/api/v1/wallet/bet", vendorLog.getOperatorEndPoints());
            assertEquals("legacy-request", vendorLog.getOperatorData());
            assertEquals("legacy-response", vendorLog.getOperatorResponse());
        }
    }

    @Nested
    @DisplayName("enrich and guards")
    class Misc {

        @Test
        @DisplayName("enrich stamps the correlating ids, write-once")
        void enrichStampsIds() {
            HttpRequestLog log = new HttpRequestLog();

            WalletRequestLog.enrich(log, "40581949", "2044577169934");
            WalletRequestLog.enrich(log, "other", "other");

            assertEquals("40581949", log.getVendorBetId());
            assertEquals("2044577169934", log.getRoundId());
        }

        @Test
        @DisplayName("the wrapper's whole sequence survives a missing LogContext")
        void missingLogContextDoesNotBreakTheWalletCall() {
            // What both wrappers now do when LogContextHolder.get() returned null: a logging
            // concern must not abort a wallet transaction.
            assertDoesNotThrow(() -> {
                HttpRequestLog log = WalletRequestLog.begin(null, REQUEST_TYPE);
                WalletRequestLog.enrich(log, "40581949", "2044577169934");
                WalletRequestLog.end(null, log);
            });
            assertNull(WalletRequestLog.begin(null, REQUEST_TYPE));
        }

        @Test
        @DisplayName("nulls are no-ops, never an exception on the wallet path")
        void nullsAreSafe() {
            assertDoesNotThrow(() -> WalletRequestLog.begin(null, REQUEST_TYPE));
            assertDoesNotThrow(() -> WalletRequestLog.end(null, new HttpRequestLog()));
            assertDoesNotThrow(() -> WalletRequestLog.end(contextWithOperatorCall(), null));
            assertDoesNotThrow(() -> WalletRequestLog.declareVendorOwned(null, new HttpRequestLog()));
            assertDoesNotThrow(() -> WalletRequestLog.enrich(null, "a", "b"));
        }
    }

    @Nested
    @DisplayName("fail: the error a failed request is published with")
    class Fail {

        @Test
        @DisplayName("records the failure the way HttpService.logError does")
        void recordsTheFailure() {
            HttpRequestLog log = new HttpRequestLog();
            IllegalStateException failure = new IllegalStateException("operator timed out");

            WalletRequestLog.fail(log, failure);

            assertEquals(failure.toString(), log.getErrorMessage());
            assertEquals("operator timed out", log.getExceptionMessage());
        }

        @Test
        @DisplayName("is write-once, so a vendor's own error is kept")
        void keepsAnErrorAlreadyRecorded() {
            HttpRequestLog log = new HttpRequestLog();
            log.setErrorMessage("vendor recorded this");

            WalletRequestLog.fail(log, new IllegalStateException("later"));

            assertEquals("vendor recorded this", log.getErrorMessage());
        }

        @Test
        @DisplayName("never throws on a missing log or failure")
        void nullSafe() {
            assertDoesNotThrow(() -> WalletRequestLog.fail(null, new IllegalStateException("x")));
            assertDoesNotThrow(() -> WalletRequestLog.fail(new HttpRequestLog(), null));
        }
    }
}
