package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;

public interface WalletBalanceService {
    public PlayerBalanceData process(BalanceContext context);
}
