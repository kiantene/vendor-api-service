package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public interface AuthenticateVendorResponseMapper<R> extends VendorResponseMapper<AuthenticateContext, R> {
    @Override
    R toVendor(AuthenticateContext context, PlayerBalanceData balanceData);
}
