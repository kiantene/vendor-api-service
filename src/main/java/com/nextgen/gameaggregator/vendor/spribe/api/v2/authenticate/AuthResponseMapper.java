package com.nextgen.gameaggregator.vendor.spribe.api.v2.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.BalanceResponse;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseMapper implements AuthenticateVendorResponseMapper<BalanceResponse> {
    @Override
    public BalanceResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        BalanceResponse.Data data = BalanceResponse.Data.builder()
                .userId(balanceData.getUsername())
                .username(balanceData.getUsername())
                .currency(balanceData.getCurrency())
                .balance(AmountConverter.convertBalanceToUnit(balanceData.getBalance()))
                .build();

        return new BalanceResponse(data);
    }
}
