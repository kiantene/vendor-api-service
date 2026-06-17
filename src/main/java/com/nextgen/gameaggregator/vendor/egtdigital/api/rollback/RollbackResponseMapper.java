package com.nextgen.gameaggregator.vendor.egtdigital.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.egtdigital.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.egtdigital.util.Amount;
import org.springframework.stereotype.Component;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<RollbackResponse> {

    @Override
    public RollbackResponse toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {


        return RollbackResponse.builder()
                .balance(Amount.vendor(balanceData.getBalance()))
                .casinoTransferId(context.getVendorBetId())
                .statusCode(ResponseCodes.OK.getCode())
                .build();
    }
}
