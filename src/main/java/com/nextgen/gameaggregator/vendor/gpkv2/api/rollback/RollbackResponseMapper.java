package com.nextgen.gameaggregator.vendor.gpkv2.api.rollback;


import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.gpkv2.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkv2.vo.CommonVo;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component
public class RollbackResponseMapper implements BetRollbackVendorResponseMapper<CommonVo> {

    @Override
    public CommonVo toVendor(BetRollbackContext context, PlayerBalanceData balanceData) {

        return CommonVo.builder()
                .code(ResponseCodes.SUCCESS.getCode())
                .balance(String.valueOf(balanceData.getBalance().setScale(4, RoundingMode.DOWN)))
                .player_id(balanceData.getUsername())
                .timestamp(balanceData.getTimestamp())
                .build();
    }
}
