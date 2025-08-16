package com.nextgen.gameaggregator.vendor.aviatorstudio.validator;

import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.core.common.AbstractVendorSignatureValidator;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.core.service.VendorPlayerDataService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.response.ErrorResponse;
import com.nextgen.gameaggregator.vendor.aviatorstudio.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AviatorStudioSignatureValidator extends AbstractVendorSignatureValidator {
    private record JwtAuthData(String token, String username) {}
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String REQUEST_ATTR_TOKEN = "token";
    private static final String REQUEST_ATTR_USERNAME = "username";
    protected AviatorStudioSignatureValidator(VendorPlayerDataService vendorPlayerDataService,
                                              VendorLineService vendorLineService) {
        super(vendorPlayerDataService, vendorLineService);
    }

    @Override
    public String getVendorClassName() {
        return EndPoints.CLASS_NAME;
    }

    @Override
    public void validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String jwtAuth = extractAuthorizationHeader(request);

        try {
            JwtAuthData authData = extractJwtClaims(jwtAuth);
            String jwtSecret = getCredentialValue(authData.username, Credentials.JWT_SECRET);
            JwtUtil.verifyAndDecode(jwtAuth, jwtSecret);
            setRequestAttributes(request, authData);
        } catch (SignatureValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SignatureValidationException("JWT validation failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        return new VendorErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, new ErrorResponse(ResponseCode.SERVER_ERROR));
    }

    private String extractAuthorizationHeader(HttpServletRequest request) throws SignatureValidationException {
        String jwtAuth = request.getHeader(HEADER_AUTHORIZATION);
        if (jwtAuth == null || jwtAuth.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }
        return jwtAuth;
    }

    private JwtAuthData extractJwtClaims(String jwtAuth) throws SignatureValidationException {
        try {
            String token = JwtUtil.getClaim(jwtAuth, JwtUtil.CLAIM_TOKEN);
            String username = JwtUtil.getClaim(jwtAuth, JwtUtil.CLAIM_USER_ID);
            return new JwtAuthData(token, username);
        } catch (Exception ex) {
            throw new SignatureValidationException("Failed to extract JWT claims: " + ex.getMessage(), ex);
        }
    }

    private void setRequestAttributes(HttpServletRequest request, JwtAuthData authData) {
        request.setAttribute(REQUEST_ATTR_TOKEN, authData.token());
        request.setAttribute(REQUEST_ATTR_USERNAME, authData.username());
    }
}
