package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class BalanceContext extends VendorRequestContext {

    /**
     * populated by enrichByGameSession in WalletBalanceServiceWrapper
     */
    private String playerIp;
    private Long vendorPlayerId;
}
