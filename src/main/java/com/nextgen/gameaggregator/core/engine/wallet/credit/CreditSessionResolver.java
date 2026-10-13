package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Decides what a credit is resolved against: the debit it names, or the player's game session.
 *
 * <p><b>Debit first.</b> A credit that names a debit is paying back into, or settling, money that
 * debit took. Everything the operator call needs about the player was recorded on the debit when it
 * ran ({@link DebitTxn#getAgentMeta()}, the same snapshot {@code GameRound.agentMeta} holds for the
 * bet/result engine), so the session adds nothing. Depending on it made a credit fail for reasons
 * unrelated to the money:
 * <ul>
 *   <li>a settle after the session aged out got {@code GameSessionExpiredException} for a debit
 *       that is still open;</li>
 *   <li>a refund had to regenerate a session and mint a throwaway token, and for an unknown uid the
 *       regeneration failed as an internal error (JDB {@code 9999} instead of {@code 7501},
 *       OVI-3180).</li>
 * </ul>
 *
 * <p><b>Session otherwise</b>, unchanged: a credit naming no debit (another vendor, or a promo win),
 * or naming only debits written before the snapshot existed, or naming debits that are all missing
 * (the policy then rejects it as bet-not-found, as before).
 */
@Service
@RequiredArgsConstructor
public class CreditSessionResolver {

    private final GameSessionDataService gameSessionDataService;
    private final DebitReferenceGuard debitReferenceGuard;

    /**
     * Loads the debits the credit names (once; {@link CreditPolicy} reuses them) and resolves the
     * credit against them or against the session.
     *
     * @throws GameSessionExpiredException when the request's player is not the one the named debits
     *                                     belong to, or (session path) no session can be found
     */
    public CreditSession resolve(CreditContext context) {
        List<KvDoc<DebitTxn>> referenced = debitReferenceGuard.load(context);
        Optional<DebitTxn> anchor = referenced.stream()
                .filter(Objects::nonNull)
                .map(KvDoc::getPayload)
                .filter(debit -> debit.getAgentMeta() != null)
                .findFirst();
        if (anchor.isEmpty()) {
            GameSession gameSession = context.isRefund()
                    ? gameSessionDataService.getOrCreate(context, true)
                    : gameSessionDataService.getGameSession(context);
            return new CreditSession(gameSession, null, referenced);
        }

        requireSamePlayer(context, referenced);
        DebitTxn debit = anchor.get();
        return new CreditSession(sessionView(debit), debit.getAgentMeta(), referenced);
    }

    /**
     * Every named debit must belong to the request's player. Compared verbatim, as the session
     * lookup it replaces does: {@code " tm63ux7pc "} is not {@code "tm63ux7pc"}. Answered as a player
     * that cannot be found (JDB {@code 7501}), the same answer a debit or settle gets for a uid with no
     * session.
     */
    private static void requireSamePlayer(CreditContext context, List<KvDoc<DebitTxn>> referenced) {
        String requestPlayer = context.getVendorPlayerUsername();
        for (KvDoc<DebitTxn> debitKv : referenced) {
            if (debitKv == null) {
                continue;
            }
            DebitTxn debit = debitKv.getPayload();
            if (!Objects.equals(requestPlayer, debit.getVendorPlayerUsername())) {
                throw new GameSessionExpiredException(context, "Credit player '" + requestPlayer
                        + "' does not own referenced debit " + debit.getId());
            }
        }
    }

    /**
     * The debit, in the shape {@code enrichByGameSession} and {@code VendorRequestValidator} read:
     * so the credit's currency, player and game code are checked against the debit exactly as they
     * would be against a session. Never persisted.
     */
    private static GameSession sessionView(DebitTxn debit) {
        AgentMeta agentMeta = debit.getAgentMeta();
        GameSession view = new GameSession();
        view.setVendorPlayerUsername(debit.getVendorPlayerUsername());
        view.setVendorCurrencyCode(debit.getVendorCurrency());
        view.setVendorGameCode(debit.getVendorGameCode());
        view.setCurrencyCode(agentMeta.getCurrency());
        view.setAgentId(agentMeta.getAgentId());
        view.setAgentPlayerUsername(agentMeta.getUsername());
        view.setGameCode(agentMeta.getGameCode());
        // The operator bet_credit call requires a token for every vendor. A debit always had one
        // (DebitContextEnricher sources it from the session); the fallback only keeps that contract.
        view.setToken(agentMeta.getSession() != null ? agentMeta.getSession() : UUID.randomUUID().toString());
        return view;
    }

    /**
     * @param gameSession  the player's session, or a view of the referenced debit
     * @param debitSnapshot the referenced debit's {@link AgentMeta} when resolved against a debit,
     *                      else {@code null}
     * @param referencedDebits as {@link DebitReferenceGuard#load} returns them
     */
    public record CreditSession(GameSession gameSession,
                                AgentMeta debitSnapshot,
                                List<KvDoc<DebitTxn>> referencedDebits) {
    }
}
