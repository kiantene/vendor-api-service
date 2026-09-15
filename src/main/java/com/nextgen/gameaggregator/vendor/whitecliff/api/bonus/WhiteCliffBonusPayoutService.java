package com.nextgen.gameaggregator.vendor.whitecliff.api.bonus;

import com.nextgen.gameaggregator.core.engine.promo.payout.AbstractPromoPayoutController;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.whitecliff.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.whitecliff.vo.ResponseVo;
import org.springframework.stereotype.Service;

/**
 * Credits a WhiteCliff {@code /bonus} through {@code /v1/promo/payout}.
 *
 * <p>Replaces the {@code walletService.processBetResult(..., ResultType.BET_WIN, ...)} call the endpoint
 * used to make. That route booked a bonus as a bet win, so it landed in bet history with a positive
 * win/loss and no turnover behind it — vendor-side GGR exceeded operator-side, which is the
 * reconciliation gap ONEAPI-358 exists to close. The payout now lands in {@code promo_payout_history}
 * with its {@code promo_type} instead.
 *
 * <p>No {@code configure()} override: WhiteCliff sends no campaign reference, so there is no resolve
 * strategy to pick and {@code populateCampaign} skips resolution entirely.
 *
 * <p>WhiteCliff is not registered in {@code Vendors} and has no {@code VendorIntegrationConfig}, so
 * {@code RequestLoggingFilter} never stamps a vendor class name onto the log context. This class sets it
 * itself — {@code PromoPayoutServiceImpl} keys the duplicate-request guard on that value, and a null
 * would namespace every idempotency key under "null".
 */
@Service
public class WhiteCliffBonusPayoutService extends AbstractPromoPayoutController<BonusPayoutRequest, ResponseVo> {

    public WhiteCliffBonusPayoutService(BonusPayoutRequestMapper requestMapper,
                                        BonusPayoutResponseMapper responseMapper,
                                        PromoPayoutService promoPayoutService) {
        super(requestMapper, responseMapper, promoPayoutService);
    }

    public ResponseVo payout(BonusPayoutRequest request, HttpRequestLog httpRequestLog) {
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
