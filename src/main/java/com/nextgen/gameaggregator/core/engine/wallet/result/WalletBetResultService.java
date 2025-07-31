package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public interface WalletBetResultService {
    PlayerBalanceData process(BetResultContext context);
}
