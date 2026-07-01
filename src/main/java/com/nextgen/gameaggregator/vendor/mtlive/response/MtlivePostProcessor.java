package com.nextgen.gameaggregator.vendor.mtlive.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.InvalidRequestContext;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.mtlive.api.adjustment.AdjustmentRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.balance.BalanceRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.betandresult.BetAndResultRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.mtlive.config.MtliveConfig;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.util.VendorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MtlivePostProcessor implements VendorResponsePostProcessor {
    private final VendorLineService vendorLineService;

    // List of request classes this processor handles
    private static final List<Class<?>> REQUEST_CLASSES = List.of(
            BalanceRequest.class,
            BetRequest.class,
            BetResultRequest.class,
            BetAndResultRequest.class,
            RollbackRequest.class,
            AdjustmentRequest.class
    );

    public MtlivePostProcessor(VendorLineService vendorLineService) {
        this.vendorLineService = vendorLineService;
    }

    @Override
    public String getVendorClassName() {
        return MtliveConfig.CLASS_NAME;
    }

    @Override
    public VendorErrorResponse postProcessErrorResponse(VendorErrorResponse errorResponse, VendorExceptionContext errorContext) {
        log.debug("Post-processing error response for vendor: {}", getVendorClassName());

        return errorContext.getAnyPresentClass(REQUEST_CLASSES).map(request -> {
            try {
                String systemCode = (String) request.getClass().getMethod("getSystem_code").invoke(request);
                String webId = (String) request.getClass().getMethod("getWeb_id").invoke(request);

                Integer vendorLineId = resolveVendorLineId(systemCode, webId);
                VendorCredentialAccessor accessor = new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));

                ResponseEntity<String> encryptedResponse = VendorUtil.encryptResponse(errorResponse.getBody(), accessor);

                HttpStatus status = HttpStatus.resolve(encryptedResponse.getStatusCode().value());

                return new VendorErrorResponse(status, encryptedResponse.getBody());

            } catch (Exception e) {
                log.error("Failed to extract system_code or encrypt response", e);
                return errorResponse;
            }
        }).orElse(errorResponse);
    }

    @Override
    public VendorErrorResponse postProcessInvalidRequest(InvalidRequestContext ctx) {
        try {
            String systemCode = ctx.getParsedFields().get("system_code");
            String webId = ctx.getParsedFields().get("web_id");

            Integer vendorLineId = resolveVendorLineId(systemCode, webId);
            VendorCredentialAccessor accessor = new VendorCredentialAccessor(vendorLineService.mapCredentialsByName(vendorLineId));

            ResponseEntity<String> encryptedResponse = VendorUtil.encryptResponse(ctx.getResponseBody(), accessor);

            Map<String, String> headers = new HashMap<>();
            encryptedResponse.getHeaders().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    headers.put(key, values.get(0));
                }
            });

            return new VendorErrorResponse((HttpStatus) encryptedResponse.getStatusCode(), encryptedResponse.getBody(), headers);

        } catch (Exception e) {
            log.error("Failed to extract system_code or encrypt response", e);
            return new VendorErrorResponse(ctx.getResponseBody());
        }
    }

    private Integer resolveVendorLineId(String systemCode, String webId) throws CredentialNotFoundException {
        Integer vendorLineId = null;

        if (systemCode != null) {
            vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.SYSTEM_CODE, systemCode);
        }

        if (vendorLineId == null && webId != null) {
            vendorLineId = vendorLineService.getVendorLineIdByNameAndValue(Credentials.WEB_ID, webId);
        }

        return vendorLineId;
    }
}
