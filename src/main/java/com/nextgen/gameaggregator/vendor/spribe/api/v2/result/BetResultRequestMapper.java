package com.nextgen.gameaggregator.vendor.spribe.api.v2.result;

import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContextMapper;
import com.nextgen.gameaggregator.vendor.spribe.constant.FreeBetAction;
import com.nextgen.gameaggregator.vendor.spribe.utils.AmountConverter;
import org.springframework.stereotype.Component;

@Component
public class BetResultRequestMapper implements BetResultContextMapper<BetResultRequest> {
    @Override
    public BetResultContext toInternal(BetResultRequest request) {
        return BetResultContext.builder()
                .idempotencyKey(request.getProviderTxId())
                .vendorBetId(request.getWithdrawProviderTxId())
                .roundId(request.getActionId())
                .vendorGameCode(request.getGame())
                .vendorPlayerUsername(request.getUserId())
                .vendorCurrency(request.getCurrency())
                .winAmount(AmountConverter.convertUnitToBalance(request.getAmount())) // amount is in thousands (no decimals)
                .vendorSessionToken(request.getSessionToken())
                .isFreeSpin(FreeBetAction.list.contains(request.getAction()) ? 1 : 0)
                .build();
    }
}
