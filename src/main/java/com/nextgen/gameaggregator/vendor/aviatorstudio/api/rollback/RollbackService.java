package com.nextgen.gameaggregator.vendor.aviatorstudio.api.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.result.BetResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RollbackService {
    private final RollbackRequestMapper requestMapper;
    private final RollbackResponseMapper responseMapper;
    private final WalletRollbackServiceWrapper walletService;

    public BetResultResponse doRollback(BetResultRequest request, String token, String username) {
        BetRollbackContext context = requestMapper.toInternal(request);
        enrich(context, token, username);
        PlayerBalanceData balanceData = walletService.process(context);
        return responseMapper.toVendor(context, balanceData);
    }

    private void enrich(BetRollbackContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
