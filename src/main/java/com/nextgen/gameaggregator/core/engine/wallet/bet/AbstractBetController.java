package com.nextgen.gameaggregator.core.engine.wallet.bet;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;

public abstract class AbstractBetController<Q, R> {
    protected final VendorRequestMapper<BetContext, Q> requestMapper;
    protected final VendorResponseMapper<BetContext, R> responseMapper;
    protected final WalletBetService walletBetService;

    protected AbstractBetController(VendorRequestMapper<BetContext, Q> requestMapper,
                                    VendorResponseMapper<BetContext, R> responseMapper,
                                    WalletBetService walletBetService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.walletBetService = walletBetService;
    }

    protected R processRequest(Q request) {
        return doBet(request);
    }

    protected BetContext mapToInternal(Q request) {
        return requestMapper.toInternal(request);
    }

    protected R mapToVendor(BetContext context, PlayerBalanceData balanceData) {
        return responseMapper.toVendor(context, balanceData);
    }

    protected final R doBet(Q request) {
        BetContext context = mapToInternal(request);
        PlayerBalanceData balanceData = walletBetService.process(context);
        return mapToVendor(context, balanceData);
    }
}
