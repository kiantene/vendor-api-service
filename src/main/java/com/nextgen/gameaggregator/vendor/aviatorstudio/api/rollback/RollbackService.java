package com.nextgen.gameaggregator.vendor.aviatorstudio.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.RollbackType;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.aviatorstudio.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RollbackService {
    private final RollbackRequestMapper requestMapper;
    private final RollbackResponseMapper responseMapper;
    private final WalletRollbackServiceWrapper walletService;

    public SuccessResponse doRollback(BetResultRequest request) {
        BetRollbackContext context = requestMapper.toInternal(request);
        PlayerBalanceData balanceData = walletService
                .initialise(context)
                .configure(config -> config.setRollbackType(RollbackType.BY_BET))
                .process();
        return responseMapper.toVendor(context, balanceData);
    }
}
