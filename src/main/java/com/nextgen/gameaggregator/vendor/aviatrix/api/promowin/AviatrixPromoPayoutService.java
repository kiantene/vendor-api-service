package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.aviatrix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import org.springframework.stereotype.Service;

/**
 * Credits an Aviatrix promoWin through {@code /v1/promo/payout}.
 *
 * <p>Replaces the {@code walletService.processBetResult} call the endpoint used to make behind the
 * {@code vendor.aviatrix.promoWinEnabled} toggle. That route booked the promo as a zero-stake bet win
 * and inflated GGR; this one records the payout in {@code promo_payout_history} with its
 * {@code promo_type} and needs no game session, which is what lets promoWin be honoured after the
 * session token has expired as Aviatrix's spec requires.
 *
 * <p><b>No campaign is resolved, for either promo type.</b> Attaching a campaign requires one to exist
 * in {@code ga-promo-engine}, and OneAPI has no Aviatrix promo integration — no campaign is created, no
 * player is registered, no grant is provisioned. {@code USERNAME_AND_BONUS_ID} was tried and cannot work:
 * it matches {@code freeRoundBonusId} against {@code CampaignPlayer.vendorGrantRef}, which only holds ids
 * the vendor returned to us at provisioning time (Groove's {@code template_id}, Dot Connections'
 * {@code freespin_id}). Aviatrix's {@code promo.bonusId} is minted by Aviatrix's own promo system, so no
 * row can ever carry it and the lookup failed silently on every bonus — {@code FetchCampaignResponseMapper}
 * turns a failed resolve into an empty {@code Campaign} rather than an error.
 *
 * <p>With no strategy and no {@code vendorCampaignCode}, {@code PromoPayoutContextEnricher.populateCampaign}
 * returns before calling the promo engine at all. {@code promoType} is then the whole description of the
 * credit, which is exactly what {@code PromoPayoutDto#promoType} documents for vendor-run promotions.
 *
 * <p>Aviatrix is not registered in {@code Vendors} and has no {@code VendorIntegrationConfig}, so
 * {@code RequestLoggingFilter} never stamps a vendor class name onto the log context. This class sets
 * it itself — {@code PromoPayoutServiceImpl} keys the duplicate-request guard on that value. Adding a
 * config bean instead would flip every Aviatrix endpoint to the new framework at once, because
 * {@code AbstractVendorConfig.isNewFramework()} is hardcoded to true.
 */
@Service
public class AviatrixPromoPayoutService extends AbstractPromoPayoutController<PromoWinDto, ResponseVo> {

    public AviatrixPromoPayoutService(PromoWinRequestMapper requestMapper,
                                      PromoWinResponseMapper responseMapper,
                                      PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public ResponseVo payout(PromoWinDto request, HttpRequestLog httpRequestLog) {
        ensureLogContext(httpRequestLog);
        return processRequest(request, context -> context.setHttpRequestLog(httpRequestLog));
    }

    private void ensureLogContext(HttpRequestLog httpRequestLog) {
        LogContext logContext = LogContextHolder.get();
        if (logContext == null) {
            logContext = new LogContext();
            LogContextHolder.set(logContext);
        }
        if (logContext.getVendorClassName() == null) {
            logContext.setVendorClassName(EndPoints.CLASS_NAME);
        }
        if (httpRequestLog != null && httpRequestLog.getId() != null) {
            logContext.setTraceId(httpRequestLog.getId());
        }
    }
}
