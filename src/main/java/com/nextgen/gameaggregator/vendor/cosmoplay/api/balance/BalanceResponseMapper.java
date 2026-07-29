package com.nextgen.gameaggregator.vendor.cosmoplay.api.balance;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateContext;
import com.nextgen.gameaggregator.core.engine.game.authenticate.AuthenticateVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.cosmoplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.cosmoplay.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class BalanceResponseMapper implements AuthenticateVendorResponseMapper<BalanceResponse> {
    @Override
    public BalanceResponse toVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        Long balance = Amount.vendor(balanceData.getBalance());

        return BalanceResponse.builder()
                .gameID(context.getVendorGameCode())
                .balance(balance)
                .isWalletIntegrated(false)
                .decimalPlace(Credentials.DEFAULT_DECIMAL_PLACE)
                .build();
    }
}
