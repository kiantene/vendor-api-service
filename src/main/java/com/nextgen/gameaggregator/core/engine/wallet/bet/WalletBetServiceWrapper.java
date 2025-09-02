package com.nextgen.gameaggregator.core.engine.wallet.bet;

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
import com.nextgen.gameaggregator.enums.TxnStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.service.business.GameTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class WalletBetServiceWrapper implements WalletBetService {
    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "bet";
    private final DuplicateRequestGuard guard;
    private final GameSessionDataService gameSessionDataService;
    private final GameTransactionService gameTransactionService;
    private final WalletBetValidator walletBetValidator;
    private final WalletService walletService;
    private final BetResultDataMapper betResultDataMapper;
    private final WalletExceptionTranslator walletExceptionTranslator;

    @Override
    public PlayerBalanceData process() {
        return process(state().getBetContext());
    }

    @Override
    public PlayerBalanceData process(BetContext context) {
        LogContext logContext = LogContextHolder.get().setLogGroup(LOG_GROUP).setType(ACTION);
        HttpRequestLog httpRequestLog = LogContextService.toHttpRequestLog(logContext);

        try {
            context.setVendorId(logContext.getVendorId());

//            guard.ensureNotDuplicate(logContext.getVendorClassName(), ACTION, context.getIdempotencyKey());
            GameTransaction txn = guard.ensureNotDuplicate(context.getVendorId(), context.getIdempotencyKey());

            enrich(context, txn);

            walletBetValidator.validateRequestContext(context);

            GameSession gameSession = gameSessionDataService.getGameSession(context);

            enrichByGameSession(context, gameSession);

            walletBetValidator.validateBusinessState(gameSession, context);

            return processBetTransaction(context, gameSession, httpRequestLog);

        } catch (DuplicateRequestException ex) {
            return handleDuplicateRequest(context, ex);
        } catch (Exception ex) {
            // TODO: need to mark error or timeout?
            guard.clear();
            walletExceptionTranslator.translateAndThrow(ex);
        } finally {
            guard.cleanup();
            LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
        }
        return null;
    }

    private void enrich(BetContext context, GameTransaction txn) {
        if (context.getVendorBetId() == null) {
            context.setVendorBetId(context.getIdempotencyKey());
        }
        if (context.getTimestamp() == null) {
            context.setTimestamp(System.currentTimeMillis());
        }
        txn.setRoundId(context.getRoundId());
        txn.setGameCode(context.getVendorGameCode());
        txn.setCurrency(context.getVendorCurrency());
        txn.setBetAmount(context.getBetAmount());
        txn.setBetTime(context.getTimestamp());
        txn.setStatus(TxnStatus.SENT);

        context.setGameTransaction(txn);
    }

    private void enrichByGameSession(BetContext context, GameSession gameSession) {
        // null check is done in gameSessionDataService.getGameSession, so we won't do null check here
        if (context.getVendorPlayerUsername() == null) {
            context.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
        }
        if (context.getVendorCurrency() == null) {
            context.setVendorCurrency(gameSession.getVendorCurrencyCode());
        }
    }

    private PlayerBalanceData handleDuplicateRequest(BetContext context, DuplicateRequestException ex) {
        GameTransaction txn = ex.getTransaction();
        if (state().getConfig().isReturnSuccessOnDuplicate() && txn.isSuccess()) {
//            return PlayerBalanceData.getDefault(context.getTraceId(), context.getVendorPlayerUsername(), context.getVendorCurrency());
            return new PlayerBalanceData(
                    txn.getUsername(),
                    txn.getCurrency(),
                    txn.getBalance(),
                    System.currentTimeMillis()
            );
        }
        throw ex;
    }

    private PlayerBalanceData processBetTransaction(
            BetContext context,
            GameSession gameSession,
            HttpRequestLog httpRequestLog) throws
                InvalidAgentApiCredentialException, VendorCurrencyNotSupportException,
                BetResultIdempotentViolationException, InsufficientBalanceException,
                TransactionStillProcessingException, InvalidOperatorResponseException,
                CouchbaseDataIntegrityException {

        gameTransactionService.markSent(context.getGameTransaction());
        BetEvent betEvent = walletService.processBet(
                httpRequestLog.getId(),
                gameSession,
                betResultDataMapper.toBetResultData(context),
                httpRequestLog.getRequestBody(),
                httpRequestLog
        );
        gameTransactionService.markSuccess(context.getGameTransaction(), betEvent.getLastBalance());

        return new PlayerBalanceData(
                context.getVendorPlayerUsername(),
                context.getVendorCurrency(),
                betEvent.getLastBalance(),
                httpRequestLog.getOperatorEnd()
        );
    }

    @Override
    public WalletBetService initialise(BetContext context) {
        BetWrapperContext state = new BetWrapperContext(context);
        BetContextHolder.set(state);
        return this;
    }

    private BetWrapperContext state() {
        return BetContextHolder.getRequired();
    }

    @Override
    public WalletBetService configure(Consumer<BetConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }
}
