package com.nextgen.gameaggregator.vendor.evoplay.api.v2.bet;

import org.springframework.stereotype.Component;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetContext;
import com.nextgen.gameaggregator.core.engine.wallet.bet.BetVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ResponseCodes;

@Component
public class BetResponseMapper implements BetVendorResponseMapper<ResponseVo> {
    @Override
    public ResponseVo toVendor(BetContext context, PlayerBalanceData balanceData) {
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
