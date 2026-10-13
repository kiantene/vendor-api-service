package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.core.exception.GameSessionExpiredException;
import com.nextgen.gameaggregator.core.validator.BetValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletDebitValidatorTest {

    @Mock
    private BetValidator betValidator;

    @InjectMocks
    private WalletDebitValidator validator;

    private DebitContext contextWith(String username) {
        return DebitContext.builder()
                .vendorPlayerUsername(username)
                .vendorTxnId("transfer-1")
                .idempotencyKey("transfer-1")
                .build();
    }

    @Test
    void validateRequestContext_rejects_null_username() {
        DebitContext ctx = contextWith(null);
        assertThrows(InvalidRequestException.class,
                () -> validator.validateRequestContext(ctx),
                "Null vendorPlayerUsername must fail request-shape validation");
    }

    @Test
    void validateRequestContext_rejects_null_vendorTxnId() {
        // vendorTxnId is half the idempotency doc id. A null one would collapse every such
        // request onto "jdb::DEBIT::null", so with returnSuccessOnDuplicate one player's
        // debit would replay another player's recorded balance.
        DebitContext ctx = DebitContext.builder()
                .vendorPlayerUsername("alice")
                .vendorTxnId(null)
                .build();

        assertThrows(InvalidRequestException.class,
                () -> validator.validateRequestContext(ctx));
    }

    @Test
    void validateRequestContext_rejects_blank_vendorTxnId() {
        DebitContext ctx = DebitContext.builder()
                .vendorPlayerUsername("alice")
                .vendorTxnId("   ")
                .build();

        assertThrows(InvalidRequestException.class,
                () -> validator.validateRequestContext(ctx));
    }

    @Test
    void validateRequestContext_passes_with_username() {
        DebitContext ctx = contextWith("alice");
        validator.validateRequestContext(ctx);
    }

    @Test
    void validateBusinessState_delegates_to_BetValidator_with_session_and_username() {
        DebitContext ctx = contextWith("alice");
        GameSession session = new GameSession();
        doNothing().when(betValidator).validateBusinessState(session, "alice", ctx);

        validator.validateBusinessState(session, ctx);

        verify(betValidator).validateBusinessState(session, "alice", ctx);
    }

    @Test
    void validateBusinessState_propagates_session_expired() {
        DebitContext ctx = contextWith("alice");
        GameSession session = new GameSession();
        doThrow(new GameSessionExpiredException()).when(betValidator)
                .validateBusinessState(session, "alice", ctx);

        assertThrows(GameSessionExpiredException.class,
                () -> validator.validateBusinessState(session, ctx));
    }

    @Test
    void validateBusinessState_propagates_bet_not_allowed() {
        DebitContext ctx = contextWith("alice");
        GameSession session = new GameSession();
        doThrow(new BetNotAllowedException()).when(betValidator)
                .validateBusinessState(session, "alice", ctx);

        assertThrows(BetNotAllowedException.class,
                () -> validator.validateBusinessState(session, ctx));
        verify(betValidator).validateBusinessState(session, "alice", ctx);
        // No business-logic short-circuit; full delegation
        verify(betValidator, never()).validateBusinessState(null, null, null);
    }
}
