package com.nextgen.gameaggregator.core.engine.wallet.credit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.Consumer;

/** Fluent service entry point used by {@link AbstractCreditController}. */
public interface WalletCreditService {
    WalletCreditService initialise(CreditContext context);
    WalletCreditService configure(Consumer<CreditConfig> configurer);
    PlayerBalanceData process();
}
