package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BalanceContext implements GameSessionData {
    private String vendorPlayerUsername;
    private String vendorCurrency;
    private String vendorSessionToken;
    private String token;
}
