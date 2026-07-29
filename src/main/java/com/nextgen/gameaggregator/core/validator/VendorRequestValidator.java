package com.nextgen.gameaggregator.core.validator;

import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.core.context.VendorRequestContext;
import com.nextgen.gameaggregator.core.vendor.config.VendorConfigService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VendorRequestValidator {

    private final VendorConfigService vendorConfigService;

    public VendorRequestValidator(VendorConfigService vendorConfigService) {
        this.vendorConfigService = vendorConfigService;
    }

    //validate using game session
    public void validateVendorRequestWithGameSession(GameSession gameSession, VendorRequestContext context) {

        validateCurrency(gameSession, context);

        validateUsername(gameSession, context);

        validateGameCode(gameSession, context);
    }

    private void validateCurrency(GameSession gameSession, VendorRequestContext context) {
        String vendorCurrency = context.getVendorCurrency() != null ? context.getVendorCurrency().trim().toUpperCase() : null;
        String sessionCurrency = gameSession.getVendorCurrencyCode() != null ? gameSession.getVendorCurrencyCode().trim().toUpperCase() : null;

        if (StringUtils.isNotBlank(vendorCurrency) && !vendorCurrency.equals(sessionCurrency)) {
            log.error("[MISMATCH] VendorCurrency：Session: {}, Request: {}, TraceId: {}",
                    sessionCurrency, vendorCurrency, context.getTraceId());
            throw new InvalidRequestException("Invalid VendorCurrency");
        }
    }

    private void validateUsername(GameSession gameSession, VendorRequestContext context) {
        String vendorPlayerUsername = context.getVendorPlayerUsername() != null ? context.getVendorPlayerUsername().trim() : null;
        String sessionVendorPlayerUsername = gameSession.getVendorPlayerUsername() != null ? gameSession.getVendorPlayerUsername().trim() : null;

        if (StringUtils.isNotBlank(vendorPlayerUsername) && !vendorPlayerUsername.equals(sessionVendorPlayerUsername)) {
            log.error("[MISMATCH] VendorPlayerUsername: Session: {}, Request: {}, TraceId: {}",
                    sessionVendorPlayerUsername, vendorPlayerUsername, context.getTraceId());
            throw new InvalidRequestException("Invalid VendorPlayerUsername");
        }
    }

    private void validateGameCode(GameSession gameSession, VendorRequestContext context) {
        String vendorGameCode = context.getVendorGameCode() != null ? context.getVendorGameCode().trim() : null;
        String sessionVendorGameCode = gameSession.getVendorGameCode() != null ? gameSession.getVendorGameCode().trim() : null;
        if (StringUtils.isNotBlank(vendorGameCode) && !vendorGameCode.equals(sessionVendorGameCode)) {
            log.warn("[MISMATCH] VendorGameCode: TraceId: {}, Request: {}, Session: {}", context.getTraceId(), vendorGameCode, sessionVendorGameCode);

            if (isGameCodeValidateEnabled(context.getVendorClassName())) {
                log.error("[REJECT] GameCode validation enforced. TraceId: {}", context.getTraceId());
                throw new InvalidRequestException("Invalid VendorGameCode");
            }
        }
    }

    private boolean isGameCodeValidateEnabled(String vendorClassName) {
        try {
            return vendorConfigService.isGameCodeValidationEnabled(vendorClassName);
        } catch (Exception e) {
            log.error("Failed to fetch game code validation config for vendor: {}", vendorClassName, e);
            return false;
        }
    }

}