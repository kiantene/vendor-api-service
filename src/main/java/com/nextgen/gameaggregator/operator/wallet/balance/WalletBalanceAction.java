package com.nextgen.gameaggregator.operator.wallet.balance;

import com.nextgen.gameaggregator.vo.OperatorResponseVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletBalanceAction {
    public OperatorResponseVo<WalletBalanceData> call(WalletBalanceDto dto) {
        OperatorResponseVo<WalletBalanceData> responseVo = new OperatorResponseVo<>();

        WalletBalanceData walletBetData = new WalletBalanceData();
        walletBetData.setBalance(new BigDecimal("1000"));

        responseVo.setData(walletBetData);

        return responseVo;
    }
}
