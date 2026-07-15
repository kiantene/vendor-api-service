package com.nextgen.gameaggregator.vendor.evolutionv2.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceProcessor;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evolutionv2.api.promo.PromoPayoutRequestDto;
import com.nextgen.gameaggregator.vendor.evolutionv2.api.promo.PromoPayoutRequestMapper;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Evolution v2 promo-payout integration.
 */
@Component
@RequiredArgsConstructor
public class EvolutionResponsePostProcessor implements VendorResponsePostProcessor {
    private final PromoPayoutRequestMapper requestMapper;
    private final BalanceProcessor balanceProcessor;

    @Override
    public VendorErrorResponse postProcessErrorResponse(
            VendorErrorResponse errorResponse,
            VendorExceptionContext errorContext) {

        errorContext.getClass(PromoPayoutRequestDto.class).ifPresent(request -> {
            if (errorResponse.getBody() instanceof ResponseVo response) {
                response.setUuid(request.getUuid());
                if (response.getResponseCode() == ResponseCode.OK) {
                    enrichDuplicateResponse(response, request);
                }
            }
        });

        return errorResponse;
    }

    private void enrichDuplicateResponse(ResponseVo response, PromoPayoutRequestDto request) {
        try {
            PromoPayoutContext context = requestMapper.toInternal(request);
            context.setVendorClassName(EndPoints.CLASS_NAME);

            LogContext logContext = LogContextHolder.get();
            String traceId = logContext == null ? request.getUuid() : logContext.getTraceId();
            PlayerBalanceData balanceData = balanceProcessor.process(traceId, context);
            response.setBalance(balanceData.getBalance());
        } catch (Exception ex) {
            response.setResponseCode(ResponseCode.TEMPORARY_ERROR);
        }
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }
}
