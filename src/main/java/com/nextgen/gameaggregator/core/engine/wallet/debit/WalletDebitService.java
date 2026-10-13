package com.nextgen.gameaggregator.core.engine.wallet.debit;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.Consumer;

/**
 * Fluent service entry point used by {@link AbstractDebitController}.
 * Mirrors {@link com.nextgen.gameaggregator.core.engine.wallet.bet.WalletBetService}.
 */
public interface WalletDebitService {
    WalletDebitService initialise(DebitContext context);
    WalletDebitService configure(Consumer<DebitConfig> configurer);
    PlayerBalanceData process();
}
