package com.nextgen.gameaggregator.operator.wallet.rollback;

import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.WalletBaseAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WalletRefundAction extends WalletBaseAction {

    public WalletRefundAction() {
        this.endpoint = EndPoints.WALLET_ROLLBACK;
        this.timeout = EndPoints.TIMEOUT;
        this.requestType = WalletRefundAction.class.getSimpleName();
    }
}
