package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Data;

@Data
public class BetResultConfig {
    private ResultType resultType;
    private SettleType settleType = SettleType.BET; // Default is settled by bet
}
