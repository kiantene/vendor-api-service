package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.game.GameSessionData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthenticateContext implements GameSessionData {
    private String token;
    private String vendorPlayerUsername;
    private String vendorCurrency;
    private String vendorSessionToken;
}
