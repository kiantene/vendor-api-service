package com.nextgen.gameaggregator.vendor.digitain.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.digitain.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.digitain.api.betandresult.BetAndResultRequest;
import com.nextgen.gameaggregator.vendor.digitain.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.digitain.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.digitain.config.DigitainConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class DigitainPostProcessor implements VendorResponsePostProcessor {

    // List of request classes this processor handles
    private static final List<Class<?>> REQUEST_CLASSES = List.of(
            BetRequest.class,
            BetAndResultRequest.class,
            BetResultRequest.class,
            RollbackRequest.class
    );

    @Override
    public VendorErrorResponse postProcessErrorResponse(
            VendorErrorResponse response,
            VendorExceptionContext context) {

        Map<String, String> headers = new HashMap<>();
        String secretKey = DigitainConfig.HEADER_AUTHORIZATION;
        context.getHeader(secretKey.toLowerCase())
                .ifPresent(value -> headers.put(secretKey, value));


        // Get the first request in the context that matches the allowed types
        Optional<Object> requestOpt = context.getAnyPresentClass(REQUEST_CLASSES);

        requestOpt.ifPresent(request -> {
            ErrorResponse errorResponse = (ErrorResponse) response.getBody();

            if (request instanceof BetRequest betRequest) {
                enrichErrorResponse(errorResponse, betRequest);
            } else if (request instanceof BetResultRequest betResultRequest) {
                enrichErrorResponse(errorResponse, betResultRequest);
            } else if (request instanceof BetAndResultRequest betAndResultRequest) {
                enrichErrorResponse(errorResponse, betAndResultRequest);
            } else if (request instanceof RollbackRequest rollbackRequest) {
                enrichErrorResponse(errorResponse, rollbackRequest);
            } else {
                log.warn("No enrichment applied for request type {} and response type {}",
                        request.getClass(), response.getClass());
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
        return DigitainConfig.CLASS_NAME;
    }

    private void enrichErrorResponse(ErrorResponse response, BetRequest betRequest) {
        response.setTxid(betRequest.getTxid());
        response.setOtxid(betRequest.getTxid());
        response.setPid(betRequest.getPid());
        response.setRid(betRequest.getRid());
    }

    private void enrichErrorResponse(ErrorResponse response, BetResultRequest betResultRequest) {
        response.setTxid(betResultRequest.getTxid());
        response.setOtxid(betResultRequest.getTxid());
        response.setPid(betResultRequest.getPid());
        response.setRid(betResultRequest.getRid());
    }

    private void enrichErrorResponse(ErrorResponse response, BetAndResultRequest betAndResultRequest) {
        response.setTxid(betAndResultRequest.getTxid());
        response.setOtxid(betAndResultRequest.getTxid());
        response.setPid(betAndResultRequest.getPid());
        response.setRid(betAndResultRequest.getRid());
    }

    private void enrichErrorResponse(ErrorResponse response, RollbackRequest rollbackRequest) {
        response.setTxid(rollbackRequest.getOtxid());
        response.setOtxid(rollbackRequest.getOtxid());
        response.setPid(rollbackRequest.getPid());
        response.setRid(rollbackRequest.getRid());
    }
}