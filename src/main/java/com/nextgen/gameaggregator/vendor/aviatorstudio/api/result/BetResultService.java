package com.nextgen.gameaggregator.vendor.aviatorstudio.api.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BetResultService {
    private final BetResultRequestMapper requestMapper;
    private final BetResultResponseMapper responseMapper;
    private final WalletBetResultServiceWrapper walletService;

    public ResponseEntity<BetResultResponse> doSettle(BetResultRequest request, String token, String username) {
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
