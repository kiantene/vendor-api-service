package com.nextgen.gameaggregator.vendor.mtlive.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.InvalidRequestContext;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.mtlive.api.adjustment.AdjustmentRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.balance.BalanceRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.bet.BetRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.betandresult.BetAndResultRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.result.BetResultRequest;
import com.nextgen.gameaggregator.vendor.mtlive.api.rollback.RollbackRequest;
import com.nextgen.gameaggregator.vendor.mtlive.config.MtliveConfig;
import com.nextgen.gameaggregator.vendor.mtlive.util.VendorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MtlivePostProcessor implements VendorResponsePostProcessor {
    private final VendorUtil vendorUtil;

    // List of request classes this processor handles
    private static final List<Class<?>> REQUEST_CLASSES = List.of(
            BalanceRequest.class,
            BetRequest.class,
            BetResultRequest.class,
            BetAndResultRequest.class,
            RollbackRequest.class,
            AdjustmentRequest.class
    );

    public MtlivePostProcessor(VendorUtil vendorUtil) {
        this.vendorUtil = vendorUtil;
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
                // Strictly resolve encryption key using user_id from the DTO
                String username = extractUsername(request);
                if (username == null || username.isBlank()) {
                    log.warn("Cannot encrypt error response: user_id is missing from request object class {}",
                            request.getClass().getSimpleName());
                    return errorResponse;
                }

                ResponseEntity<String> encryptedResponse = vendorUtil.encryptResponse(errorResponse.getBody(), username);
                HttpStatus status = HttpStatus.resolve(encryptedResponse.getStatusCode().value());
                return new VendorErrorResponse(status, encryptedResponse.getBody());

            } catch (Exception e) {
                log.error("Failed to encrypt error response for vendor: {}", getVendorClassName(), e);
                return errorResponse;
            }
        }).orElse(errorResponse);
    }

    @Override
    public VendorErrorResponse postProcessInvalidRequest(InvalidRequestContext ctx) {
        try {
            Map<String, String> parsedFields = ctx.getParsedFields();

            // Always derive encryption key access from user_id via player lookup.
            String username = parsedFields != null ? parsedFields.get("user_id") : null;
            if (username == null || username.isBlank()) {
                log.warn("Cannot encrypt invalid request response: user_id missing from parsed fields");
                return new VendorErrorResponse(ctx.getResponseBody());
            }

            ResponseEntity<String> encryptedResponse = vendorUtil.encryptResponse(ctx.getResponseBody(), username);

            Map<String, String> headers = new HashMap<>();
            encryptedResponse.getHeaders().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    headers.put(key, values.get(0));
                }
            });

            return new VendorErrorResponse((HttpStatus) encryptedResponse.getStatusCode(), encryptedResponse.getBody(), headers);

        } catch (Exception e) {
            log.error("Failed to encrypt invalid request response for vendor: {}", getVendorClassName(), e);
            return new VendorErrorResponse(ctx.getResponseBody());
        }
    }

    private String extractUsername(Object request) {
        try {
            Method method = request.getClass().getMethod("getUser_id");
            Object result = method.invoke(request);
            return result != null ? result.toString() : null;
        } catch (NoSuchMethodException e) {
            log.warn("Request DTO class {} does not implement getUser_id(). Error responses for this request type will go out unencrypted.",
                    request.getClass().getSimpleName());
        } catch (ReflectiveOperationException e) {
            log.error("Could not reflectively invoke getUser_id on request object class {}: {}",
                    request.getClass().getSimpleName(), e.getMessage(), e);
        }
        return null;
    }
}