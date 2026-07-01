package com.nextgen.gameaggregator.vendor.smartsoft.api.settle;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.settled.BetResultData;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.dto.TransactionInfoDto;
import com.nextgen.gameaggregator.vendor.smartsoft.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GA-14424 — Smartsoft settle (/Withdraw) "no force-success" resend bug.
 *
 * Bug: SettleAction did a balance call BEFORE settling; an operator error there was caught by the generic
 * handler and returned HTTP 500 + empty {} — which made Smartsoft resend the result. Fix: the normal settle
 * path no longer calls the operator balance endpoint at all; the balance is needed only as a fallback on an
 * idempotent resend, where it's taken from the stored result and a live read is done lazily only if missing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettleActionTest {

    @InjectMocks private SettleAction settleAction;

    @Mock private WalletService              walletService;
    @Mock private HttpService                httpService;
    @Mock private VendorService              vendorService;
    @Mock private VendorLineService          vendorLineService;
    @Mock private RequestIdempotentLogService requestIdempotentLogService;

    @Mock private HttpServletRequest request;
    @Mock private SettleDto          settleDto;
    @Mock private TransactionInfoDto txnInfo;
    @Mock private GameSession        gameSession;
    @Mock private BetInformation     betInformation;

    private HttpRequestLog httpRequestLog; // real, so the lazy getCurrentBalance copy-ctor works

    private MockedStatic<HttpService>     httpStatic;
    private MockedStatic<ValidationUtils> validationStatic;
    private MockedStatic<VendorService>   vendorStatic;

    private static final String CLOSE_ROUND_BODY =
            "{\"TransactionId\":\"d9e89275\",\"TransactionType\":\"CloseRound\",\"Amount\":0.0,\"CurrencyCode\":\"ETB\",\"TransactionInfo\":{}}";

    @BeforeEach
    void setUp() throws Exception {
        httpStatic       = mockStatic(HttpService.class);
        validationStatic = mockStatic(ValidationUtils.class); // validateRequest + isEquals -> no-op
        vendorStatic     = mockStatic(VendorService.class);   // signatureGenerator -> no-op

        httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");
        httpRequestLog.setRequestBody(CLOSE_ROUND_BODY);

        when(httpService.start(request)).thenReturn(httpRequestLog);
        httpStatic.when(() -> HttpService.convertJsonToDto(anyString(), eq(SettleDto.class))).thenReturn(settleDto);

        when(settleDto.getUserName()).thenReturn("player001");
        when(settleDto.getClientExternalKey()).thenReturn("player001");
        when(settleDto.getSignature()).thenReturn("sig");
        when(settleDto.getTransactionInfoDto()).thenReturn(txnInfo);
        when(txnInfo.getGameName()).thenReturn("JetX");

        when(requestIdempotentLogService.checkExists(any(BetResultData.class), any())).thenReturn(null);
        when(vendorService.getHeaders(request)).thenReturn("");
        when(vendorService.checkGameSession(any(), any(), any())).thenReturn(gameSession);
        when(gameSession.getVendorPlayerUsername()).thenReturn("player001");
        when(vendorService.calculateResultType(any(), any(), any(), anyBoolean())).thenReturn(ResultType.END);
        when(vendorLineService.getCredentialValueByName(any(), any())).thenReturn("secret");
    }

    @AfterEach
    void tearDown() {
        httpStatic.close();
        validationStatic.close();
        vendorStatic.close();
    }

    @Test
    @DisplayName("GA-14424: normal settle returns 200 and NEVER calls the operator balance endpoint")
    void settle_returns200_andNeverCallsOperatorBalance() throws Exception {
        when(walletService.processBetResult(any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("100.00"));

        ResponseEntity<SettleVo> resp = settleAction.withdrawTransaction(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(new BigDecimal("100.00"), resp.getBody().getBalance());
        assertEquals("trace-1", resp.getBody().getTransactionId());
        // Regression: previously a balance pre-fetch ran here and a failure produced 500 + {} -> resends.
        verify(walletService, never()).getBalance(any(), any(), any());
    }

    @Test
    @DisplayName("GA-14424: idempotent resend returns the STORED balance without a live balance read")
    void idempotentResend_returnsStoredBalance_withoutLiveFetch() throws Exception {
        when(betInformation.getBalance()).thenReturn(new BigDecimal("88.67"));
        when(betInformation.getInternalTransactionId()).thenReturn("txn-1");
        BetResultIdempotentViolationException idempotent = new BetResultIdempotentViolationException(betInformation);
        when(walletService.processBetResult(any(), any(), any(), any(), any(), any())).thenThrow(idempotent);

        ResponseEntity<SettleVo> resp = settleAction.withdrawTransaction(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("txn-1", resp.getBody().getTransactionId());
        assertEquals(new BigDecimal("88.67"), resp.getBody().getBalance());
        verify(walletService, never()).getBalance(any(), any(), any());
    }

    @Test
    @DisplayName("GA-14424: idempotent resend with no stored balance lazily reads the live balance")
    void idempotentResend_storedBalanceNull_lazilyFetchesLiveBalance() throws Exception {
        when(betInformation.getBalance()).thenReturn(null);
        when(betInformation.getInternalTransactionId()).thenReturn("txn-2");
        BetResultIdempotentViolationException idempotent = new BetResultIdempotentViolationException(betInformation);
        when(walletService.processBetResult(any(), any(), any(), any(), any(), any())).thenThrow(idempotent);
        when(walletService.getBalance(any(), any(), any())).thenReturn(new BigDecimal("50.00"));

        ResponseEntity<SettleVo> resp = settleAction.withdrawTransaction(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(new BigDecimal("50.00"), resp.getBody().getBalance());
        verify(walletService, times(1)).getBalance(any(), any(), any()); // fetched only because stored was missing
    }

    @Test
    @DisplayName("GA-14424: lazy resend balance read failure is logged and returns an empty balance (still 200)")
    void idempotentResend_storedNull_liveFetchFails_isLoggedAndNonFatal() throws Exception {
        when(betInformation.getBalance()).thenReturn(null);
        when(betInformation.getInternalTransactionId()).thenReturn("txn-3");
        BetResultIdempotentViolationException idempotent = new BetResultIdempotentViolationException(betInformation);
        when(walletService.processBetResult(any(), any(), any(), any(), any(), any())).thenThrow(idempotent);
        when(walletService.getBalance(any(), any(), any())).thenThrow(new InvalidOperatorResponseException());

        Logger seLogger = (Logger) LoggerFactory.getLogger(SettleAction.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        seLogger.addAppender(appender);

        ResponseEntity<SettleVo> resp = settleAction.withdrawTransaction(request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNull(resp.getBody().getBalance());
        assertTrue(
                appender.list.stream().anyMatch(e ->
                        e.getLevel() == Level.WARN && e.getFormattedMessage().contains("resend balance read failed")),
                "a failed lazy resend balance read must be logged, not silently swallowed");
    }
}
