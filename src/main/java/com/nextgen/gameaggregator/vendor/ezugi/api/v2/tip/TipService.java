package com.nextgen.gameaggregator.vendor.ezugi.api.v2.tip;

import org.springframework.stereotype.Service;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.result.BetResultContext;
import com.nextgen.gameaggregator.core.engine.wallet.result.WalletBetResultServiceWrapper;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.ezugi.api.v2.bet.BetResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TipService {
    private final TipRequestMapper requestMapper;
    private final TipResponseMapper responseMapper;
    private final WalletBetResultServiceWrapper walletService;

    public BetResponse doTip(BetRequest request) {
        BetResultContext context = requestMapper.toInternal(request);
        PlayerBalanceData balanceData = walletService
                .initialise(context)
                .configure(config -> config.betAndResult(true))
                .process();
        BetResponse response = responseMapper.toVendor(context, balanceData);
        return enrichResponse(response, request);
    }

    private BetResponse enrichResponse(BetResponse response, BetRequest request) {
        response.setOperatorId(request.getOperatorId());
        response.setRoundId(request.getRoundId());
        return response;
    }
}
