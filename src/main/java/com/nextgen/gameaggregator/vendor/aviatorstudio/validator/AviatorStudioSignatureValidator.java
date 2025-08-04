package com.nextgen.gameaggregator.vendor.aviatorstudio.validator;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.Credentials;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AviatorStudioSignatureValidator implements VendorSignatureValidator {
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String CLAIM_USER_ID = "userId";
    private final VendorPlayerService vendorPlayerService;
    private final VendorLineService vendorLineService;

    @Override
    public String getVendorClassName() {
        return "aviator";
    }

    @Override
    public void validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) throws SignatureValidationException {
        String jwtAuth = request.getHeader(HEADER_AUTHORIZATION);

        if (jwtAuth == null || jwtAuth.isBlank()) {
            throw new SignatureValidationException("Missing Authorization header");
        }

        try {
            String vendorPlayerUsername = getUsername(jwtAuth);
            request.setAttribute("username", vendorPlayerUsername);
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerUsername);
            Integer vendorLineId = vendorPlayer.getVendorLineId();
            String jwtSecret = vendorLineService.getCredentialValueByName(vendorLineId, Credentials.JWT_SECRET);
            verifyAndDecode(jwtAuth, jwtSecret);

        } catch (Exception ex) {
            throw new SignatureValidationException(ex.getMessage(), ex);
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseCode(ResponseCode.SERVER_ERROR);

        return new VendorErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseVo);
    }

    private DecodedJWT verifyAndDecode(String jwtToken, String jwtSecret) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(jwtToken);
    }

    public static String getUsername(String jwtToken) {
        DecodedJWT decodedJWT = JWT.decode(jwtToken);
        return decodedJWT.getClaim(CLAIM_USER_ID).asString();
    }
}
