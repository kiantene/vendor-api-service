package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceProcessor;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.service.SettledBetDataService;
import com.nextgen.gameaggregator.core.validator.VendorRequestValidator;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameRoundService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class WalletRollbackServiceWrapperTest {

    private final WalletRollbackServiceWrapper wrapper = new WalletRollbackServiceWrapper(
            mock(DuplicateRequestGuard.class),
            mock(BetRollbackContextEnricher.class),
            mock(GameRoundService.class),
            mock(SettledBetDataService.class),
            mock(RollbackDataMapper.class),
            mock(BalanceProcessor.class),
            mock(BetRollbackProcessor.class),
            mock(WalletService.class),
            mock(WalletExceptionTranslator.class),
            mock(LogContextService.class),
            mock(GameTransactionService.class),
            mock(GameSessionDataService.class),
            mock(VendorRequestValidator.class)
    );

    private void invokeEnrichHttpRequestLog(HttpRequestLog httpRequestLog, BetRollbackContext context) throws Exception {
        Method method = WalletRollbackServiceWrapper.class.getDeclaredMethod(
                "enrichHttpRequestLog", HttpRequestLog.class, BetRollbackContext.class);
        method.setAccessible(true);
        method.invoke(wrapper, httpRequestLog, context);
    }

    @Test
    void enrichHttpRequestLog_contextHasNoHttpRequestLogYet_wiresSameInstanceOntoContext() throws Exception {
        // Without this, the synchronous rollback path (BetRollbackProcessor.processRollbackByBet
        // -> callToOperator) has no httpRequestLog to write a failure onto — see ONEAPI-618's
        // v2 counterpart: enrichByGameSession()/enrichAsync() already did this for their own
        // paths, but the plain sync path never did.
        HttpRequestLog httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-1");

        BetRollbackContext context = BetRollbackContext.builder()
                .traceId("trace-1")
                .vendorBetId("bet-1")
                .roundId("round-1")
                .build();

        invokeEnrichHttpRequestLog(httpRequestLog, context);

        assertSame(httpRequestLog, context.getHttpRequestLog());
    }

    @Test
    void enrichHttpRequestLog_contextAlreadyHasHttpRequestLog_doesNotOverwriteIt() throws Exception {
        // enrichByGameSession()/enrichAsync() may have already wired one on (async path);
        // the sync-path fix must not clobber it.
        HttpRequestLog existing = new HttpRequestLog();
        existing.setId("existing");

        BetRollbackContext context = BetRollbackContext.builder()
                .traceId("trace-1")
                .httpRequestLog(existing)
                .build();

        HttpRequestLog fresh = new HttpRequestLog();
        fresh.setId("fresh");

        invokeEnrichHttpRequestLog(fresh, context);

        assertSame(existing, context.getHttpRequestLog());
    }

    @Test
    void enrichHttpRequestLog_nullHttpRequestLog_doesNotThrowOrTouchContext() throws Exception {
        BetRollbackContext context = BetRollbackContext.builder().traceId("trace-1").build();

        invokeEnrichHttpRequestLog(null, context);

        assertNull(context.getHttpRequestLog());
    }
}
