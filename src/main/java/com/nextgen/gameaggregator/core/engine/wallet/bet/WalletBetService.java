package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

import java.util.function.Consumer;

public interface WalletBetService {
    WalletBetService initialise(BetContext context);
    WalletBetService configure(Consumer<BetConfig> configurer);
    PlayerBalanceData process(BetContext context);
    PlayerBalanceData process();
}
