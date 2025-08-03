package com.nextgen.gameaggregator.core.engine.game.authenticate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticateContext {
    private String vendorPlayerUsername;
    private String vendorCurrency;
    private String vendorSessionToken;
}
