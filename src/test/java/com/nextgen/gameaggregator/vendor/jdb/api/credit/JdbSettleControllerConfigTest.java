package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditConfig;
import com.nextgen.gameaggregator.core.engine.wallet.credit.WalletCreditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the CreditConfig that JDB Action 10 runs under.
 *
 * <p>The credit pipeline's only idempotency key is the credit's own transferId, so a settle that
 * re-credits an already-settled debit under a fresh transferId reads as a brand-new request
 * (OVI-3185), and one naming debits from two different rounds is accepted outright (OVI-3188).
 * {@code requireDebitReference} plus the always-on debit check is what closes both.
 */
@ExtendWith(MockitoExtension.class)
class JdbSettleControllerConfigTest {

    @Mock private JdbSettleContextMapper requestMapper;
    @Mock private JdbCreditResponseMapper responseMapper;
    @Mock private WalletCreditService walletService;

    private CreditConfig configuredConfig() {
        JdbSettleController controller =
                new JdbSettleController(requestMapper, responseMapper, walletService);
        CreditConfig config = new CreditConfig();
        controller.configure(config, new SettleActionDto());
        return config;
    }

    @Test
    @DisplayName("a settle is bound to the debits it names")
    void bindsToReferencedDebits() {
        assertTrue(configuredConfig().isRequireDebitReference(),
                "without this a spent debit can be re-credited under a new transferId (OVI-3185) "
                        + "and a cross-round settle is accepted (OVI-3188)");
    }

    @Test
    @DisplayName("the other Action 10 opt-ins are unchanged")
    void keepsTheOtherOptIns() {
        CreditConfig config = configuredConfig();

        assertTrue(config.isReturnSuccessOnDuplicate(),
                "an idempotent replay must return 0000 + prior balance, not DUPLICATE_TRANSACTION");
        assertFalse(config.isAllowCreditWhenRoundEnded(),
                "JDB rejects a settle on a round whose ledger already ended");
    }
}
