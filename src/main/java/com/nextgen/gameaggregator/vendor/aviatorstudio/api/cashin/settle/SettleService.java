package com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.settle;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInRequest;
import com.nextgen.gameaggregator.vendor.aviatorstudio.api.cashin.CashInResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettleService {
    private final SettleRequestMapper requestMapper;
    private final SettleResponseMapper responseMapper;
    private final WalletBetResultServiceWrapper walletService;

    public ResponseEntity<CashInResponse> doSettle(CashInRequest request, String token, String username) {
        BetResultContext context = requestMapper.toBetResultContext(request);
        enrich(context, token, username);
        PlayerBalanceData balanceData = walletService
                .initialise(context)
                .isBetTxn(false)
                .process();
        return ResponseEntity.ok(responseMapper.toVendor(context, balanceData));
    }

    private void enrich(BetResultContext context, String token, String username) {
        context.setToken(token);
        context.setVendorPlayerUsername(username);
    }
}
