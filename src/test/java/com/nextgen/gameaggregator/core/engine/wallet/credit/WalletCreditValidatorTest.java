package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.core.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletCreditValidatorTest {

    private final WalletCreditValidator validator = new WalletCreditValidator();

    private CreditContext settleContext(String username) {
        return CreditContext.builder()
                .vendorPlayerUsername(username)
                .vendorTxnId("transfer-2")
                .idempotencyKey("transfer-2")
                .isRefund(false)
                .build();
    }

    private CreditContext refundContext(String username) {
        return CreditContext.builder()
                .vendorPlayerUsername(username)
                .vendorTxnId("transfer-3")
                .idempotencyKey("transfer-3")
                .isRefund(true)
                .build();
    }

    @Test
    void validateRequestContext_rejects_null_username() {
        CreditContext ctx = settleContext(null);
        assertThrows(InvalidRequestException.class,
                () -> validator.validateRequestContext(ctx));
    }

    @Test
    void validateRequestContext_rejects_null_vendorTxnId() {
        // vendorTxnId is half the idempotency doc id. A null one would collapse every such
        // request onto "jdb::CREDIT::null", so with returnSuccessOnDuplicate one player's
        // settle would replay another player's recorded balance.
        CreditContext ctx = CreditContext.builder()
                .vendorPlayerUsername("alice")
                .vendorTxnId(null)
                .isRefund(false)
                .build();

        assertThrows(InvalidRequestException.class,
                () -> validator.validateRequestContext(ctx));
    }

    @Test
    void validateRequestContext_rejects_blank_vendorTxnId() {
        CreditContext ctx = CreditContext.builder()
                .vendorPlayerUsername("alice")
                .vendorTxnId("   ")
                .isRefund(true)
                .build();

        assertThrows(InvalidRequestException.class,
                () -> validator.validateRequestContext(ctx));
    }

    @Test
    void validateRequestContext_passes_for_settle() {
        validator.validateRequestContext(settleContext("alice"));
    }

    @Test
    void validateRequestContext_passes_for_refund() {
        validator.validateRequestContext(refundContext("alice"));
    }
}
