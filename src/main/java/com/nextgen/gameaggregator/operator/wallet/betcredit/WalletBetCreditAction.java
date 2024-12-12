package com.nextgen.gameaggregator.operator.wallet.betcredit;

import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.WalletBaseAction;
import org.springframework.stereotype.Service;

@Service
public class WalletBetCreditAction extends WalletBaseAction {
    public WalletBetCreditAction() {
        this.endpoint = EndPoints.WALLET_BET_CREDIT;
        this.timeout = EndPoints.TIMEOUT;
        this.requestType = WalletBetCreditAction.class.getSimpleName();
    }
}
