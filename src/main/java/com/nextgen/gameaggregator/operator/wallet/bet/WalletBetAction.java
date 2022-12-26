package com.nextgen.gameaggregator.operator.wallet.bet;

import com.nextgen.gameaggregator.vo.OperatorResponseVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletBetAction {
    public OperatorResponseVo<WalletBetData> call(WalletBetDto dto) {
        OperatorResponseVo<WalletBetData> responseVo = new OperatorResponseVo<>();

        WalletBetData walletBetData = new WalletBetData();
        walletBetData.setBalance(new BigDecimal("1000"));

        responseVo.setData(walletBetData);

        return responseVo;
    }
}
