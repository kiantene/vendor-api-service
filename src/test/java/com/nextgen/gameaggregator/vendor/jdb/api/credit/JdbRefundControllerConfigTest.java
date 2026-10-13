package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditConfig;
import com.nextgen.gameaggregator.core.engine.wallet.credit.WalletCreditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the CreditConfig that JDB Action 11 runs under.

 *
 * <p>OVI-3184 was once pinned here: the {@code allowRefundBeforeDebit} opt-out let a cancel
 * naming a debit GA never recorded answer 0000 and move money. That flag no longer exists — the
 * upstream-debit check is unconditional and covered by {@code CreditPolicyTest}.
 */
@ExtendWith(MockitoExtension.class)
class JdbRefundControllerConfigTest {

    @Mock private JdbRefundContextMapper requestMapper;
    @Mock private JdbCreditResponseMapper responseMapper;
    @Mock private WalletCreditService walletService;

    private CreditConfig configuredConfig() {
        JdbRefundController controller =
                new JdbRefundController(requestMapper, responseMapper, walletService);
        CreditConfig config = new CreditConfig();
        controller.configure(config, new RefundActionDto());
        return config;
    }

    @Test
    @DisplayName("a cancel is bound to the debits it names")
    void bindsToReferencedDebits() {
        assertTrue(configuredConfig().isRequireDebitReference(),
                "without this the same debit can be cancelled twice under different transfer ids "
                        + "(OVI-2167), and a credit can still follow a cancel (OVI-3187)");
    }

    @Test
    @DisplayName("the other Action 11 opt-ins are unchanged")
    void keepsTheOtherOptIns() {
        CreditConfig config = configuredConfig();

        assertTrue(config.isReturnSuccessOnDuplicate(),
                "an idempotent replay must return 0000 + prior balance, not DUPLICATE_TRANSACTION");
    }
}
