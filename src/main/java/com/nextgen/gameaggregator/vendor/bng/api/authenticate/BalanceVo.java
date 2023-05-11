package com.nextgen.gameaggregator.vendor.bng.api.authenticate;

import lombok.Data;

@Data
public class BalanceVo {

    private String value;      // Identifier of the user within the Casino Operator’s system
    private String version;    // Currency of the player
}
