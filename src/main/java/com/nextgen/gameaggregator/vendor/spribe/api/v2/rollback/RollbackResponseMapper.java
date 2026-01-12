package com.nextgen.gameaggregator.vendor.spribe.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<SuccessResponse> {

    @Override
    public SuccessResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        SuccessResponse.Data data = SuccessResponse.Data.builder()
                .userId(context.getVendorPlayerUsername())
                .currency(context.getVendorCurrency())
                .newBalance(AmountConverter.convertBalanceToUnit(balanceData.getBalance()))
                .build();

        return new SuccessResponse(data);
    }
}
