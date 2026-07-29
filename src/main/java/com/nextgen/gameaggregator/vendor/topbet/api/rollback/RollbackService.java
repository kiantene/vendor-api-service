package com.nextgen.gameaggregator.vendor.topbet.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.enums.RollbackType;
import com.nextgen.gameaggregator.vendor.topbet.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.topbet.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RollbackService {
    private final RollbackServiceRequestMapper requestMapper;
    private final RollbackServiceResponseMapper responseMapper;
    private final WalletRollbackServiceWrapper walletService;

    public SuccessResponse doRollback(BetResultRequest request) {
        BetRollbackContext context = requestMapper.toInternal(request);
        PlayerBalanceData balanceData = walletService
                .initialise(context)
                .configure(config -> config.rollbackType(RollbackType.BY_ROUND).allowRollbackForSettledBet(false))
                .process();
        return responseMapper.toVendor(context, balanceData);
    }

}
