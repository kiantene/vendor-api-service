package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.WalletRequestLog;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.core.validator.VendorRequestValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Default {@link WalletDebitService} implementation. Manages per-request state in
 * {@link DebitContextHolder} and orchestrates the framework-level guards before delegating
 * to {@link DebitProcessor}.
 *
 * <p>Follows the canonical debit/credit ordering contract documented on
 * {@link com.nextgen.gameaggregator.core.engine.wallet.credit.WalletCreditServiceWrapper} — read it
 * there rather than maintaining a second copy of the list. Debit's order is a strict subset; its
 * only differences are:
 * <ul>
 *   <li><b>Session lookup is always strict</b> ({@code getGameSession}) — there is no refund mode,
 *       so no regenerate-on-expiry carveout and no token minting.</li>
 *   <li><b>{@code validateBusinessState} always runs</b> — again, no refund carveout to skip it.</li>
 *   <li><b>No policy phase.</b> There is no {@code DebitPolicy}, so {@link DebitProcessor#process}
 *       was already self-sufficient and its {@code tryReplay} is a pure optimisation rather than
 *       (as on the credit side) also the thing that keeps a recorded success ahead of a gate.</li>
 *   <li><b>Audit is success-only</b> and not in a {@code finally} — a failed debit writes no
 *       audit, preserving the legacy "failed bet = no bet" semantic. This is about the audit
 *       trail specifically: {@code process} does have a {@code finally}, and the request-log step
 *       in it deliberately runs on failure too, so a rejected operator call is still visible in
 *       api_request_log (OVI-3176). Logging a failure is not the same as auditing a bet.</li>
 * </ul>
 * <p>
 * Idempotency is handled by {@code DebitTxnDataService.insertOrGet} (Couchbase doc-id collision),
 * fronted by the key-only replay peek. The legacy {@code RequestIdempotentLogService} MySQL guard is
 * intentionally NOT used on this path — Couchbase doc-state is the modern equivalent, matching the
 * BET path.
 * <p>
 * No inline exception translation — processor and validators throw runtime exceptions
 * natively; they propagate unchanged to the controller's {@code @VendorExceptionHandler}-routed
 * mapper.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletDebitServiceWrapper implements WalletDebitService {

    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "debit";
    /** For HttpRequestLog/api_request_log identification. No legacy action class of this name;
     *  nothing switches on the value (only WalletBalanceAction is special-cased, in HttpService). */
    private static final String REQUEST_TYPE = "WalletDebitAction";

    private final DebitProcessor debitProcessor;
    private final GameSessionDataService gameSessionDataService;
    private final DebitContextEnricher debitContextEnricher;
    private final WalletDebitValidator walletDebitValidator;
    private final VendorRequestValidator vendorRequestValidator;

    @Override
    public WalletDebitService initialise(DebitContext context) {
        DebitContextHolder.set(new DebitWrapperContext(context));
        return this;
    }

    @Override
    public WalletDebitService configure(Consumer<DebitConfig> configurer) {
        configurer.accept(state().getConfig());
        return this;
    }

    @Override
    public PlayerBalanceData process() {
        // Guarded because LogContextHolder.get() is a bare ThreadLocal.get() and may be null.
        // The request filter always populates it for a vendor callback, but this is engine code
        // shared by every vendor on debit/credit, and a missing log context must not turn into a
        // failed wallet transaction. WalletRequestLog no-ops on a null context.
        LogContext logContext = LogContextHolder.get();
        if (logContext != null) {
            logContext.setLogGroup(LOG_GROUP).setType(ACTION);
        }
        HttpRequestLog httpRequestLog = WalletRequestLog.begin(logContext, REQUEST_TYPE);
        try {
            DebitContext context = state().getDebitContext();

            WalletRequestLog.enrich(httpRequestLog, context.getVendorTxnId(), context.getVendorRoundId());

            walletDebitValidator.validateRequestContext(context);

            // Before the session lookup: a replay of an already-completed debit must not fail on
            // state that changed after it succeeded. Notably getGameSession throws once the session
            // has aged out, which would report 7501 for a debit GA has already made.
            Optional<PlayerBalanceData> replay = debitProcessor.tryReplay(state());
            if (replay.isPresent()) {
                return replay.get();
            }

            GameSession gameSession = gameSessionDataService.getGameSession(context);

            debitContextEnricher.enrichByGameSession(context, gameSession);

            // Request-vs-session consistency: currency, username and game code. The bet,
            // result, rollback and balance wrappers all call this after enrichment; debit and
            // credit did not, so a request naming a different currency than the session was
            // simply carried through (OVI-2166). Throws core InvalidRequestException, which
            // maps to 8000.
            vendorRequestValidator.validateVendorRequestWithGameSession(gameSession, context);

            walletDebitValidator.validateBusinessState(gameSession, context);

            return debitProcessor.process(state());
        } catch (RuntimeException failure) {
            // Before the finally: WalletRequestLog.end hands a vendor-owned log back to its vendor,
            // and nothing records the error on it after this point (OVI-3176).
            WalletRequestLog.fail(httpRequestLog, failure);
            throw failure;
        } finally {
            DebitContextHolder.clear();
            // In the finally, so a failed or timed-out operator call is recorded too. Publishing is
            // decided inside: the framework publishes its own log via logApiRequest, a vendor-owned
            // one is published by the vendor and only enriched here (OVI-3176).
            WalletRequestLog.end(logContext, httpRequestLog);
        }
    }

    private DebitWrapperContext state() {
        return DebitContextHolder.getRequired();
    }
}
