package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public interface WalletBetService {
    PlayerBalanceData process(BetContext context);
}
