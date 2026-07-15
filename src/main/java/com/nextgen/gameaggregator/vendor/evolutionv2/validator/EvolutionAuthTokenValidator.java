package com.nextgen.gameaggregator.vendor.evolutionv2.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.security.signature.ValidationResult;
import com.nextgen.gameaggregator.core.security.signature.VendorSignatureValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.service.VendorService;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evolutionv2.constant.EndPoints;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Evolution v2 promo-payout integration.
 *
 * <p>Authenticates the inbound {@code promo_payout} callback by {@code sid} session token only —
 * the same scheme as the existing Evolution wallet callbacks (check/balance/debit/credit/cancel).
 * Inbound Evolution callbacks are <b>not</b> authenticated by {@code authToken}.</p>
 */
@Component
public class EvolutionAuthTokenValidator implements VendorSignatureValidator {

    private final VendorService vendorService;

    public EvolutionAuthTokenValidator(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public boolean shouldValidate(HttpServletRequest request, String endpoint) {
        return endpoint.endsWith(EndPoints.PROMO_PAYOUT);
    }

    @Override
    public ValidationResult validate(HttpServletRequest request,
                                     Map<String, String> formFields,
                                     String rawBody) throws SignatureValidationException {
        String username = formFields.get("userId");
        String sid = formFields.get("sid");
        String currency = formFields.get("currency");
        String uuid = formFields.get("uuid");

        if (username == null || sid == null || currency == null) {
            throw new EvolutionCallbackValidationException(ResponseCode.INVALID_PARAMETER, uuid);
        }

        try {
            // Authenticate the callback by sid session token (no authToken — same as the wallet callbacks).
            GameSession gameSession = vendorService.preCheckGameSessionToken(sid);
            // Reject a terminated session (status == 0), mirroring the legacy Evolution wallet callbacks.
            if (gameSession.getStatus() != null && gameSession.getStatus() == 0) {
                throw new EvolutionCallbackValidationException(ResponseCode.INVALID_SID, uuid);
            }
            // The session token resolved must match the sid presented (legacy parity, exact match).
            if (!Objects.equals(gameSession.getVendorToken(), sid)) {
                throw new EvolutionCallbackValidationException(ResponseCode.INVALID_SID, uuid);
            }
            if (!matches(gameSession.getVendorPlayerUsername(), username)
                    || !matches(gameSession.getVendorCurrencyCode(), currency)) {
                throw new EvolutionCallbackValidationException(ResponseCode.INVALID_PARAMETER, uuid);
            }
            return ValidationResult.success();
        } catch (EvolutionCallbackValidationException ex) {
            throw ex;
        } catch (AuthenticationException ex) {
            throw new EvolutionCallbackValidationException(ResponseCode.INVALID_SID, uuid);
        } catch (Exception ex) {
            throw new EvolutionCallbackValidationException(ResponseCode.UNKNOWN_ERROR, uuid);
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return errorResponse(ResponseCode.INVALID_SID, null);
    }

    @Override
    public VendorErrorResponse onInvalidSignature(SignatureValidationException exception) {
        if (exception instanceof EvolutionCallbackValidationException evolutionException) {
            return errorResponse(evolutionException.getResponseCode(), evolutionException.getUuid());
        }
        return errorResponse(ResponseCode.INVALID_SID, null);
    }

    @Override
    public boolean useNewEvents() {
        return true;
    }

    private VendorErrorResponse errorResponse(ResponseCode responseCode, String uuid) {
        ResponseVo response = new ResponseVo();
        response.setResponseCode(responseCode);
        response.setUuid(uuid);
        return new VendorErrorResponse(response);
    }

    private boolean matches(String expected, String actual) {
        return expected != null && actual != null && expected.equalsIgnoreCase(actual);
    }
}
