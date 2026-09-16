package com.nextgen.gameaggregator.core.engine.wallet.rollback;

import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.core.exception.RollbackNotAllowedException;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.entity.couchbase.GameTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RollbackPolicyTest {

    private BetRollbackConfig config;
    private GameTransaction betTxn;
    private GameRound gameRound;

    @BeforeEach
    void setUp() {
        config = Mockito.mock(BetRollbackConfig.class);
        betTxn = Mockito.mock(GameTransaction.class);
        gameRound = Mockito.mock(GameRound.class);

        // Satisfy the global precondition check
        when(config.getRollbackType()).thenReturn(RollbackType.BY_BET);

        // Default to enabling the amount validation feature flag
        when(config.isValidateAmountWithBet()).thenReturn(true);
        // Default to transaction-scoped rollback so validation logic fires by default
        when(config.isRollbackByBet()).thenReturn(true);
    }

    @Test
    @DisplayName("Should return structured reject decision when rollbackType is completely missing from config")
    void shouldReject_WhenRollbackTypeIsNull() {
        // Override setup to simulate unconfigured type boundary
        when(config.getRollbackType()).thenReturn(null);

        RollbackDecision decision = RollbackPolicy.decide(betTxn, gameRound, config, new BigDecimal("10.00"));

        // Verifies the structural architecture: avoids raw language exceptions to preserve context wrapping
        assertNotNull(decision);
        assertTrue(decision.isRejected());
        assertEquals(RollbackNotAllowedException.class, decision.exceptionClass());
        assertTrue(decision.reason().contains("rollbackType is not explicitly defined"));
    }

    @Test
    @DisplayName("Should reject rollback cleanly with vendor-visible reason when validation is active but vendor rollbackAmount is null")
    void shouldReject_WhenRollbackAmountIsNull() {
        RollbackDecision decision = RollbackPolicy.decide(betTxn, gameRound, config, null);

        assertNotNull(decision);
        assertTrue(decision.isRejected());
        assertEquals(RollbackNotAllowedException.class, decision.exceptionClass());
        assertEquals("Rollback rejected: rollbackAmount not provided", decision.reason());
    }

    @Test
    @DisplayName("Should reject rollback cleanly when DB betAmount is missing/null")
    void shouldReject_WhenStoredBetAmountIsNull() {
        when(betTxn.getBetAmount()).thenReturn(null);
        BigDecimal rollbackAmount = new BigDecimal("10.00");

        RollbackDecision decision = RollbackPolicy.decide(betTxn, gameRound, config, rollbackAmount);

        assertNotNull(decision);
        assertTrue(decision.isRejected());
        assertEquals(RollbackNotAllowedException.class, decision.exceptionClass());
        assertEquals("Rollback rejected: betAmount not available on original bet transaction", decision.reason());
    }

    @Test
    @DisplayName("Should reject rollback with generic message when transaction amounts mismatch")
    void shouldReject_WhenAmountMismatches() {
        when(betTxn.getBetAmount()).thenReturn(new BigDecimal("10.00"));
        BigDecimal incorrectRollbackAmount = new BigDecimal("5.50");

        RollbackDecision decision = RollbackPolicy.decide(betTxn, gameRound, config, incorrectRollbackAmount);

        assertNotNull(decision);
        assertTrue(decision.isRejected());
        assertEquals(RollbackNotAllowedException.class, decision.exceptionClass());

        // Explicitly ensuring we don't leak original values (like "Expected 10.00 but got 5.50")
        assertEquals("Rollback rejected: Amount Mismatch.", decision.reason());
    }

    @Test
    @DisplayName("Should pass validation and allow rollback when amounts match")
    void shouldAllow_WhenAmountsMatch() {
        BigDecimal matchingAmount = new BigDecimal("20.00");
        when(betTxn.getBetAmount()).thenReturn(matchingAmount);

        // Mocking downstream methods in RollbackPolicy.decide to let it hit an ALLOW block cleanly
        when(betTxn.isUnsettled()).thenReturn(true);
        when(betTxn.isSuccess()).thenReturn(true);

        RollbackDecision decision = RollbackPolicy.decide(betTxn, gameRound, config, matchingAmount);

        assertNotNull(decision);
        assertTrue(decision.isAllowed());
        assertFalse(decision.isRejected());

        // CRITICAL REGRESSION GUARD: Confirm that the amount comparison branch actually executed
        Mockito.verify(betTxn, Mockito.atLeastOnce()).getBetAmount();
    }

    @Test
    @DisplayName("Should skip amount validation and allow rollback when validate feature flag is disabled")
    void shouldSkipValidation_WhenFeatureFlagIsDisabled() {
        // Explicitly disable the amount validation flag
        when(config.isValidateAmountWithBet()).thenReturn(false);

        // Mock downstream status checks to ensure it reaches an ALLOW block
        when(betTxn.isUnsettled()).thenReturn(true);
        when(betTxn.isSuccess()).thenReturn(true);

        // Even though rollbackAmount is null, it should NOT reject because the block is skipped
        RollbackDecision decision = RollbackPolicy.decide(betTxn, gameRound, config, null);

        assertNotNull(decision);
        assertTrue(decision.isAllowed());
        assertFalse(decision.isRejected());
    }

    @Test
    @DisplayName("Should skip amount validation if configured for round rollbacks even if validate flag is true")
    void shouldSkipAmountValidation_WhenConfiguredForRoundRollback() {
        // Feature flag is true, but rollback scope is explicitly shifted to BY_ROUND
        when(config.getRollbackType()).thenReturn(RollbackType.BY_ROUND);
        when(config.isRollbackByBet()).thenReturn(false);

        // Mock downstream status checks to ensure an ALLOW block can be reached safely
        when(betTxn.isUnsettled()).thenReturn(true);
        when(betTxn.isSuccess()).thenReturn(true);

        // This would normally reject due to null rollbackAmount, but your self-defending check skips it smoothly
        RollbackDecision decision = RollbackPolicy.decide(betTxn, gameRound, config, null);

        assertNotNull(decision);
        assertTrue(decision.isAllowed());
        assertFalse(decision.isRejected());
    }
}