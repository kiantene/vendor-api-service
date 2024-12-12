package com.nextgen.gameaggregator.operator.wallet.betdebit;

import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.WalletBaseAction;
import org.springframework.stereotype.Service;

@Service
public class WalletBetDebitAction extends WalletBaseAction {
    public WalletBetDebitAction() {
        this.endpoint = EndPoints.WALLET_BET_DEBIT;
        this.timeout = EndPoints.TIMEOUT;
        this.requestType = WalletBetDebitAction.class.getSimpleName();
    }
}
