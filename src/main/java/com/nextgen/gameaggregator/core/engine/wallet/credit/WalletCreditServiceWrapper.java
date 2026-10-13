package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.WalletRequestLog;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.validator.VendorRequestValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Default {@link WalletCreditService} implementation, and the <b>canonical ordering contract for
 * the whole debit/credit framework</b>. Credit is the constrained side — it has both session modes,
 * both credit modes, and the policy phase — so the contract lives here and
 * {@link com.nextgen.gameaggregator.core.engine.wallet.debit.WalletDebitServiceWrapper} points at it.
 *
 * <h3>Canonical pipeline order</h3>
 *
 * Each step is annotated with the data it depends on, because <b>that is what fixes the order</b>.
 * Do not reorder without invalidating the stated dependency.
 *
 * <ol>
 *   <li><b>{@code validateRequestContext}</b> — <i>wire fields only.</i> Request shape:
 *       {@code vendorPlayerUsername} and {@code vendorTxnId} must be present. Both are preconditions
 *       of everything below.</li>
 *
 *   <li><b>{@code creditProcessor.tryReplay}</b> — <i>wire fields only</i> (the txn doc id is
 *       {@code vendorClassName} + {@code vendorTxnId}). Key-only replay peek; returns the recorded
 *       result and stops here.
 *       <p><b>Why it must precede the session lookup:</b> every guard below can fail on state that
 *       changed <i>after</i> this transaction already succeeded, which would report failure for money
 *       that has already moved. A settle replay arriving after the session TTL expires would get
 *       {@code GameSessionExpiredException} → JDB {@code 7501} "User ID cannot be found" for a credit
 *       GA has already made; a replay after the agent, player, vendor line or game was disabled would
 *       get {@code 9999}; a refund replay after the game was delisted would get
 *       {@code GameNotSupportedException} → {@code 9999}. JDB retries settle every 60s and cancel for
 *       up to 12h, so all of these are reachable. Running the peek first also means a replay never
 *       mints a throwaway session token and never pays for the enrichment chain.</li>
 *
 *   <li><b>{@code CreditSessionResolver.resolve}</b> — <i>wire fields.</i> Loads the referenced
 *       debits (kept on the wrapper state for {@code CreditPolicy}). When one carries an
 *       {@code agentMeta} snapshot, the credit is resolved against the debit: its player must own
 *       every named debit ({@code GameSessionExpiredException} otherwise), and no session is read.
 *       Otherwise — no debit named, only pre-snapshot debits, or all missing — the session lookup,
 *       asymmetric by mode:
 *     <ul>
 *       <li><b>Settle</b> — {@code getGameSession} (strict; expired sessions throw).</li>
 *       <li><b>Refund</b> — {@code getOrCreate} (regenerates when the session has expired but
 *           {@code vendorPlayerUsername} is present, so the wallet refund still settles after the
 *           player session has aged out — matches the legacy {@code CancelBetService}
 *           catch-and-regen carveout). Regeneration always mints a fresh GA token: the session
 *           was found by no identifier, so the request's token is null or unknown, and the
 *           operator {@code bet_credit} call requires one for every vendor. Legacy refunds
 *           (JDB {@code CancelBetService}, Koolbet, Booongo) likewise never forward a null
 *           token.</li>
 *     </ul>
 *   </li>
 *
 *   <li><b>{@code enrichByGameSession}</b> — backfills {@code token} / {@code vendorCurrency} /
 *       {@code vendorGameCode} / {@code currencyCode} from the session (null-fallback only), then
 *       runs the {@code BaseEnricher} chain keyed off {@code vendorPlayerUsername} to produce
 *       {@code vendorId}, {@code agentId}, {@code currencyId}, {@code vendorLineId},
 *       {@code gameCode} and the FX rates. For a debit-resolved credit, {@code applyDebitSnapshot}
 *       then pins {@code agentId} / {@code agentPlayerUsername} / {@code gameCode} on the debit's.
 *       <p><b>Why it must precede the claim (step 5):</b> {@code buildFreshTxn}'s audit population
 *       reads {@code vendorId}, {@code gameCode} and {@code currencyCode}. This is the reason the
 *       idempotency check is <i>split</i> rather than simply moved to the front.</li>
 *
 *   <li><b>{@code creditProcessor.process}</b> — <i>needs step 4.</i> Internally:
 *     <ol>
 *       <li>{@code CreditPolicy.evaluate} — <i>wire fields only,</i> so it is free to sit here.
 *           It does, deliberately: a pure read-gate must not leave a persisted document behind for
 *           a request it rejects.</li>
 *       <li>{@code CreditIdempotencyResolver.resolve} — the <b>authoritative</b> atomic
 *           {@code insertOrGet} claim. Step 2 is an advisory fast path only; {@code process()}
 *           re-peeks on policy rejection so it stays correct even when {@code tryReplay} was never
 *           called, and so the peek→claim window is closed.</li>
 *       <li>operator call, then audit-of-attempt in a {@code finally} — audit runs on BOTH the
 *           success and failure paths and never masks the operator exception.</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * <p>See {@link com.nextgen.gameaggregator.core.engine.wallet.idempotency.WalletIdempotencyResolver}
 * for the peek-vs-claim split, and {@link CreditProcessor} for the per-collaborator breakdown.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletCreditServiceWrapper implements WalletCreditService {

    private static final String LOG_GROUP = "wallet";
    private static final String ACTION = "credit";
    /** For HttpRequestLog/api_request_log identification. No legacy action class of this name;
     *  nothing switches on the value (only WalletBalanceAction is special-cased, in HttpService). */
    private static final String REQUEST_TYPE = "WalletCreditAction";

    private final CreditProcessor creditProcessor;
    private final CreditSessionResolver creditSessionResolver;
    private final CreditContextEnricher creditContextEnricher;
    private final WalletCreditValidator walletCreditValidator;
    private final VendorRequestValidator vendorRequestValidator;

    @Override
    public WalletCreditService initialise(CreditContext context) {
        CreditContextHolder.set(new CreditWrapperContext(context));
        return this;
    }

    @Override
    public WalletCreditService configure(Consumer<CreditConfig> configurer) {
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
            CreditContext context = state().getCreditContext();

            WalletRequestLog.enrich(httpRequestLog, context.getVendorTxnId(), context.getVendorRoundId());

            walletCreditValidator.validateRequestContext(context);

            // Deliberately after validateRequestContext: bad-request precedence beats duplicate
            // precedence, and the replay vendor view reads vendorPlayerUsername, which only the
            // request-shape check guarantees.
            Optional<PlayerBalanceData> replay = creditProcessor.tryReplay(state());
            if (replay.isPresent()) {
                return replay.get();
            }

            CreditSessionResolver.CreditSession session = creditSessionResolver.resolve(context);
            state().setReferencedDebits(session.referencedDebits());
            GameSession gameSession = session.gameSession();

            creditContextEnricher.enrichByGameSession(context, gameSession);
            if (session.debitSnapshot() != null) {
                creditContextEnricher.applyDebitSnapshot(context, session.debitSnapshot());
            }

            // Request-vs-session consistency: currency, username and game code. The bet,
            // result, rollback and balance wrappers all call this after enrichment; debit and
            // credit did not, so a request naming a different currency than the session was
            // simply carried through (OVI-2166). Throws core InvalidRequestException, which
            // maps to 8000.
            vendorRequestValidator.validateVendorRequestWithGameSession(gameSession, context);

            return creditProcessor.process(state());
        } catch (RuntimeException failure) {
            // Before the finally: WalletRequestLog.end hands a vendor-owned log back to its vendor,
            // and nothing records the error on it after this point (OVI-3176).
            WalletRequestLog.fail(httpRequestLog, failure);
            throw failure;
        } finally {
            CreditContextHolder.clear();
            // In the finally, so a failed or timed-out operator call is recorded too. Publishing is
            // decided inside: the framework publishes its own log via logApiRequest, a vendor-owned
            // one is published by the vendor and only enriched here (OVI-3176).
            WalletRequestLog.end(logContext, httpRequestLog);
        }
    }

    private CreditWrapperContext state() {
        return CreditContextHolder.getRequired();
    }
}
