package com.nextgen.gameaggregator.core.engine.wallet.bet;

import java.util.function.Consumer;

import com.nextgen.gameaggregator.core.validator.VendorRequestValidator;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.service.data.producer.transactionhistory.BetTransactionHistoryProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.exception.translator.WalletExceptionTranslator;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.TxnType;
import com.nextgen.gameaggregator.service.business.GameTransactionService;

import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletBetServiceWrapper implements WalletBetService {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "bet";
    private static final String REQUEST_TYPE = "WalletBetAction"; // For HttpRequestLog Backward Compatability
    private final DuplicateRequestGuard guard;
    private final BetContextEnricher enricher;
    private final GameSessionDataService gameSessionDataService;
    private final GameTransactionService gameTransactionService;
    private final WalletBetValidator walletBetValidator;
    private final WalletExceptionTranslator walletExceptionTranslator;
    private final BetLifeCycleRegistry lifeCycleRegistry;
    private final BetTransactionHistoryProducer betTransactionHistoryProducer;
    private final BetProcessor betProcessor;
    private final VendorRequestValidator vendorRequestValidator;

    @Override
    public PlayerBalanceData process() {
        return process(state().getBetContext());
    }

    @Override
    public PlayerBalanceData process(BetContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);
        httpRequestLog.setRequestType(REQUEST_TYPE);
        GameTransaction txn = null;
        String vendorClassName = logContext.getVendorClassName();

        try {
            /**
             * Enriching HttpRequestLog for backward compatability
             * TO BE REMOVED when HttpRequestLog is completely removed
             */
            enrichHttpRequestLog(httpRequestLog, context);

            txn = guard.ensureNotDuplicate(
                    TxnType.BET,
                    logContext.getVendorClassName(),
                    context.getIdempotencyKey(),
                    logContext.getStart()
            );

            walletBetValidator.validateRequestContext(context);

            GameSession gameSession = gameSessionDataService.getGameSession(context);

            enricher.enrichByGameSession(context, gameSession);
            //validate vendor request
            vendorRequestValidator.validateVendorRequestWithGameSession(gameSession, context);
            /**
             * Use the Strategy Pattern to execute vendor specific logic before the wallet call.
             * The handler is retrieved from the registry based on the transaction's vendor class name.
             * Do it before business validation so that any updates to the game session can be considered.
             */
            BetLifeCycle handler = lifeCycleRegistry.getHandler(vendorClassName);

            if (handler != null) {
                handler.onBeforeSend(gameSession, context);
            }

            walletBetValidator.validateBusinessState(gameSession, context);

            return betProcessor.processBetTransaction(state(), gameSession, txn, httpRequestLog);

        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (Exception ex) {
            RuntimeException exception = walletExceptionTranslator.translate(ex, context);
            gameTransactionService.markError(txn, exception);

            throw exception;
        } finally {
            cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
    }

    @Override
    public WalletBetService initialise(BetContext context) {
        BetWrapperContext state = new BetWrapperContext(context);
        BetContextHolder.set(state);
        return this;
    }

    @Override
    public WalletBetService configure(Consumer<BetConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    private BetWrapperContext state() {
        return BetContextHolder.getRequired();
    }

    private void cleanup() {
        guard.cleanup();
        BetContextHolder.clear();
    }

    private PlayerBalanceData handleDuplicateRequest(BetContext context, DuplicateRequestException ex) {
        GameTransaction txn = ex.getTransaction();
        if (state().getConfig().isReturnSuccessOnDuplicate() && txn.isSuccess()) {
            return new PlayerBalanceData(
                    txn.getUsername(),
                    txn.getCurrency(),
                    txn.getBalance(),
                    System.currentTimeMillis()
            );
        }
        throw ex;
    }

    private void enrichHttpRequestLog(HttpRequestLog httpRequestLog, BetContext context) {
        if (httpRequestLog != null) {
            httpRequestLog.setRequestType(REQUEST_TYPE);

            httpRequestLog.setVendorBetId(context.getVendorBetId());
            httpRequestLog.setRoundId(context.getRoundId());
        }
    }
}
