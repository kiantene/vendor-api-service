package com.nextgen.gameaggregator.vendor.digitain.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.digitain.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.digitain.api.betandresult.BetAndResultRequest;
import com.nextgen.gameaggregator.vendor.digitain.api.promowin.PromoWinRequest;
import com.nextgen.gameaggregator.vendor.digitain.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.digitain.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.digitain.config.DigitainConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DigitainPromoWinPostProcessor implements VendorResponsePostProcessor {

    // List of request classes this processor handles
    private static final List<Class<?>> REQUEST_CLASSES = List.of(
            BetRequest.class,
            BetAndResultRequest.class,
            BetResultRequest.class,
            RollbackRequest.class,
            PromoWinRequest.class
    );

    @Override
    public VendorErrorResponse postProcessErrorResponse(
            VendorErrorResponse response,
            VendorExceptionContext context) {

        Map<String, String> headers = new HashMap<>();
        String secretKey = DigitainConfig.HEADER_AUTHORIZATION;
        context.getHeader(secretKey.toLowerCase())
                .ifPresent(value -> headers.put(secretKey, value));

        context.getClass(PromoWinRequest.class).ifPresent(promoWinRequest -> {
            if (response.getBody() instanceof ErrorResponse errorResponse) {
                enrichErrorResponse(errorResponse, promoWinRequest);
            } else {
                log.warn("Body is not an ErrorResponse: {}", response.getBody().getClass());
            }
        });
        return new VendorErrorResponse(
                response.getStatusCode(),
                response.getBody(),
                headers
        );
    }

    @Override
    public String getVendorClassName() {
        return DigitainConfig.CLASS_NAME+"PromoWin";
    }

    private void enrichErrorResponse(ErrorResponse response, PromoWinRequest promoWinRequest) {
        response.setTxid(promoWinRequest.getTxid());
        response.setOtxid(promoWinRequest.getTxid());
        response.setPid(promoWinRequest.getPid());
        response.setRid("");
    }

}