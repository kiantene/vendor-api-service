package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.rollback;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.BetRollbackContext;
import com.nextgen.gameaggregator.core.engine.wallet.rollback.WalletRollbackServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInRequest;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RollbackService {
    private final RollbackRequestMapper requestMapper;
    private final RollbackResponseMapper responseMapper;
    private final WalletRollbackServiceWrapper walletService;

    public ResponseEntity<CashInResponse> doRollback(CashInRequest request, String token, String username) {
        BetRollbackContext context = requestMapper.toBetRollbackContext(request);
        enrich(context, token, username);
        PlayerBalanceData balanceData = walletService.process(context);
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetRollbackContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
