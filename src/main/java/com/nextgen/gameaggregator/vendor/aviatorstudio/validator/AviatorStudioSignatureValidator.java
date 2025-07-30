package com.nextgen.gameaggregator.vendor.aviatorstudio.validator;

import com.nextgen.gameaggregator.core.common.VendorErrorResponse;
import com.nextgen.gameaggregator.core.common.VendorSignatureValidator;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.entity.ga.VendorPlayer;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.service.VendorPlayerService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.aviatorstudio.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatorstudio.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AviatorStudioSignatureValidator implements VendorSignatureValidator {
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private final VendorPlayerService vendorPlayerService;
    private final VendorService vendorService;

    @Override
    public String getVendorClassName() {
        return "aviator";
    }

    @Override
    public boolean validate(HttpServletRequest request, Map<String, String> formFields, String rawBody) {
        String jwtAuth = request.getHeader(HEADER_AUTHORIZATION);
        LogContext logContext = LogContextHolder.get();

        if (jwtAuth == null || jwtAuth.isBlank()) {
            logContext.setErrorMessage("Missing Authorization header");
            return false;
        }

        String vendorPlayerUsername = VendorService.jwtGetUserId(jwtAuth);
        try {
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorPlayerUsername);
            Integer vendorLineId = vendorPlayer.getVendorLineId();
            vendorService.verifyJWT(jwtAuth, vendorLineId, vendorPlayerUsername);
            return true;

        } catch (InvalidPlayerException | AuthenticationException | CredentialNotFoundException ex) {
            logContext.setException(ex.getClass().getSimpleName());
            logContext.setErrorMessage(ex.getMessage());
            return false;
        }
    }

    @Override
    public VendorErrorResponse onInvalidSignature(HttpServletRequest request) {
        CommonVo responseVo = new CommonVo();
        responseVo.setResponseCode(ResponseCode.SERVER_ERROR);

        return new VendorErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseVo);
    }
}
