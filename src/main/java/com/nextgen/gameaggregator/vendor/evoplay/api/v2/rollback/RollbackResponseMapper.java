package com.nextgen.gameaggregator.vendor.evoplay.api.v2.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import org.springframework.stereotype.Component;

@Component
class RollbackResponseMapper implements BetRollbackVendorResponseMapper<ResponseVo> {
    @Override
    public ResponseVo toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {
        return ResponseVo.builder()
                .status(ResponseCodes.SUCCESS.status)
                .data(mapResponseData(balanceData))
                .build();
    }

    private ResponseDataVo mapResponseData(PlayerBalanceData balanceData){
        return ResponseDataVo.builder()
                .balance(balanceData.getBalance())
                .currency(balanceData.getCurrency())
                .build();
    }
}
