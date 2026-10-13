package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.service.GameSessionDataService;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.DebitTxn;
import com.nextgen.gameaggregator.entity.couchbase.KvDoc;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditSessionResolverTest {

    @Mock private GameSessionDataService gameSessionDataService;
    @Mock private DebitReferenceGuard debitReferenceGuard;

    @InjectMocks private CreditSessionResolver resolver;

    private static CreditContext credit(boolean refund, String uid) {
        return CreditContext.builder()
                .vendorClassName("jdb")
                .vendorTxnId("4288401826")
                .vendorRoundId("1203088843")
                .vendorPlayerUsername(uid)
                .isRefund(refund)
                .referencedVendorDebitTxnIds(List.of("7281508472"))
                .build();
    }

    private static AgentMeta agentMeta(String session) {
        AgentMeta agentMeta = new AgentMeta();
        agentMeta.setAgentId(1087);
        agentMeta.setUsername("op-tm63ux7pc");
        agentMeta.setCurrency("PHP");
        agentMeta.setGameCode("JDB_18_18029");
        agentMeta.setSession(session);
        return agentMeta;
    }

    private static KvDoc<DebitTxn> debit(String player, AgentMeta agentMeta) {
        DebitTxn debit = DebitTxn.of("jdb", "7281508472", 1L);
        debit.setVendorPlayerUsername(player);
        debit.setVendorCurrency("PP");
        debit.setVendorGameCode("18_18029");
        debit.setAgentMeta(agentMeta);
        return new KvDoc<>(debit.getId(), 7L, debit);
    }

    // ---------- Resolved against the debit ----------

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void debitWithSnapshot_resolvesAgainstTheDebit_withoutReadingAnySession(boolean refund) {
        CreditContext context = credit(refund, "tm63ux7pc");
        List<KvDoc<DebitTxn>> loaded = List.of(debit("tm63ux7pc", agentMeta("ga-token-1")));
        when(debitReferenceGuard.load(context)).thenReturn(loaded);

        CreditSessionResolver.CreditSession session = resolver.resolve(context);

        verifyNoInteractions(gameSessionDataService);
        assertSame(loaded, session.referencedDebits());
        assertNotNull(session.debitSnapshot());
        assertEquals(1087, session.debitSnapshot().getAgentId());

        GameSession view = session.gameSession();
        assertEquals("tm63ux7pc", view.getVendorPlayerUsername());
        assertEquals("PP", view.getVendorCurrencyCode(), "vendor currency, what the validator compares");
        assertEquals("PHP", view.getCurrencyCode(), "operator currency");
        assertEquals("18_18029", view.getVendorGameCode());
        assertEquals("JDB_18_18029", view.getGameCode());
        assertEquals(1087, view.getAgentId());
        assertEquals("op-tm63ux7pc", view.getAgentPlayerUsername());
        assertEquals("ga-token-1", view.getToken(), "the operator token the debit was made with");
    }

    /** OVI-3180: blank, space-padded and wrong uid all used to reach session regeneration → 9999. */
    @ParameterizedTest
    @ValueSource(strings = {"", " tm63ux7pc ", "uid"})
    void uidThatDoesNotOwnTheDebit_isAPlayerThatCannotBeFound(String uid) {
        CreditContext context = credit(true, uid);
        when(debitReferenceGuard.load(context))
                .thenReturn(List.of(debit("tm63ux7pc", agentMeta("ga-token-1"))));

        assertThrows(GameSessionExpiredException.class, () -> resolver.resolve(context));

        verifyNoInteractions(gameSessionDataService);
    }

    @Test
    void everyNamedDebitMustBelongToTheRequestPlayer() {
        CreditContext context = credit(false, "tm63ux7pc");
        when(debitReferenceGuard.load(context)).thenReturn(List.of(
                debit("tm63ux7pc", agentMeta("ga-token-1")),
                debit("someone-else", agentMeta("ga-token-2"))));

        assertThrows(GameSessionExpiredException.class, () -> resolver.resolve(context));
    }

    @Test
    void aMissingSiblingIsLeftToThePolicy() {
        // A null entry is a debit that does not exist: CreditPolicy rejects it as bet-not-found.
        // The player check must not trip on it first.
        CreditContext context = credit(false, "tm63ux7pc");
        List<KvDoc<DebitTxn>> loaded = Arrays.asList(null, debit("tm63ux7pc", agentMeta("ga-token-1")));
        when(debitReferenceGuard.load(context)).thenReturn(loaded);

        CreditSessionResolver.CreditSession session = resolver.resolve(context);

        assertNotNull(session.debitSnapshot());
        verifyNoInteractions(gameSessionDataService);
    }

    @Test
    void snapshotWithoutAToken_stillSendsOne() {
        CreditContext context = credit(true, "tm63ux7pc");
        when(debitReferenceGuard.load(context)).thenReturn(List.of(debit("tm63ux7pc", agentMeta(null))));

        GameSession view = resolver.resolve(context).gameSession();

        assertNotNull(view.getToken(), "operator bet_credit requires a token for every vendor");
    }

    // ---------- Falls back to the session ----------

    @Test
    void settleNamingNoDebit_usesTheStrictSessionLookup() {
        CreditContext context = credit(false, "tm63ux7pc");
        GameSession session = new GameSession();
        when(debitReferenceGuard.load(context)).thenReturn(List.of());
        when(gameSessionDataService.getGameSession(context)).thenReturn(session);

        CreditSessionResolver.CreditSession resolved = resolver.resolve(context);

        assertSame(session, resolved.gameSession());
        assertNull(resolved.debitSnapshot());
        verify(gameSessionDataService, never()).getOrCreate(any(), anyBoolean());
    }

    @Test
    void refundOnPreSnapshotDebit_usesGetOrCreateWithAMintedToken() {
        // Debits written before agentMeta existed carry no snapshot: keep the old path for them.
        CreditContext context = credit(true, "tm63ux7pc");
        GameSession session = new GameSession();
        when(debitReferenceGuard.load(context)).thenReturn(List.of(debit("tm63ux7pc", null)));
        when(gameSessionDataService.getOrCreate(context, true)).thenReturn(session);

        CreditSessionResolver.CreditSession resolved = resolver.resolve(context);

        assertSame(session, resolved.gameSession());
        assertNull(resolved.debitSnapshot());
    }

    @Test
    void allNamedDebitsMissing_usesTheSession() {
        CreditContext context = credit(false, "tm63ux7pc");
        when(debitReferenceGuard.load(context)).thenReturn(Arrays.asList((KvDoc<DebitTxn>) null));
        when(gameSessionDataService.getGameSession(context)).thenReturn(new GameSession());

        assertNull(resolver.resolve(context).debitSnapshot());
    }
}
