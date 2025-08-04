package com.nextgen.gameaggregator.vendor.smartsoft.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Headers;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.smartsoft.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class AuthenticateAction {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;

    public AuthenticateAction(HttpService httpService, VendorLineService vendorLineService, VendorService vendorService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.SESSION)
    public ResponseEntity<AuthenticateVo> authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.OK;
        String body = httpRequestLog.getRequestBody();
        String signature = request.getHeader(Headers.REQUEST_SIGNATURE);
        AuthenticateVo responseVo = new AuthenticateVo();
        httpRequestLog.setRequestBody("Request Body: \n" + body + "\n\nRequest Header: \n" + vendorService.getHeaders(request));

        try {
            // 1. Retrieve request body and convert into dto
            AuthenticateDto dto = HttpService.convertJsonToDto(body, AuthenticateDto.class);
            dto.setSignature(signature);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = vendorService.preCheckGameSessionToken(dto.getToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession, body, httpRequestLog.getMethod());

            String portalName = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_NAME);

            // 5. Set response data
            responseVo.setSessionId(gameSession.getToken());
            responseVo.setUserName(gameSession.getVendorPlayerUsername());
            responseVo.setClientExternalKey(gameSession.getVendorPlayerId().toString());
            responseVo.setCurrencyCode(gameSession.getVendorCurrencyCode());
            responseVo.setPortalName(portalName);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            headers.add(Headers.ERROR_CODE, ResponseCode.INTERNAL_ERROR.code.toString());
            headers.add(Headers.ERROR_MESSAGE, ResponseCode.INTERNAL_ERROR.message);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return new ResponseEntity<>(responseVo, headers, status);
    }

    private void doValidation(AuthenticateDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(AuthenticateDto dto, GameSession gameSession, String body, String method) throws AuthenticationException, CredentialNotFoundException {        // Verify received signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.signatureGenerator(secretKey, method, body), dto.getSignature(), AuthenticationException::new);
    }

}
