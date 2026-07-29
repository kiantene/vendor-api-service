package com.nextgen.gameaggregator.vendor.spribe.api.v2.result;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.vendor.spribe.response.SuccessResponse;
import org.springframework.stereotype.Component;

@Component
public class FreebetDepositHandler implements DepositActionHandler {

    private final PromoPayoutService promoPayoutService;
    private final FreebetPayoutContextMapper contextMapper;
    private final FreebetPayoutResponseMapper responseMapper;

    public FreebetDepositHandler(PromoPayoutService promoPayoutService,
                                 FreebetPayoutContextMapper contextMapper,
                                 FreebetPayoutResponseMapper responseMapper) {
        this.promoPayoutService = promoPayoutService;
        this.contextMapper = contextMapper;
        this.responseMapper = responseMapper;
    }

    @Override
    public boolean supports(String action) {
        return "freebet".equals(action);
    }

    @Override
    public SuccessResponse handle(BetResultRequest request) {
        PromoPayoutContext ctx = contextMapper.toInternal(request);
        ctx.setVendorClassName(LogContextHolder.getVendorClassName());
        PlayerBalanceData data = promoPayoutService
                .initialise(ctx)
                .configure(config -> config.playerUuidCampaignLookup(true))
                .process(ctx);
        SuccessResponse response = responseMapper.toVendor(ctx, data);
        enrich(request, response, ctx.getTraceId());
        return response;
    }

    private void enrich(BetResultRequest request, SuccessResponse response, String traceId) {
        response.getData().setOperatorTxId(traceId);
        response.getData().setProvider(request.getProvider());
        response.getData().setProviderTxId(request.getProviderTxId());
        response.getData().setOldBalance(response.getData().getNewBalance().subtract(request.getAmount()));
    }
}
