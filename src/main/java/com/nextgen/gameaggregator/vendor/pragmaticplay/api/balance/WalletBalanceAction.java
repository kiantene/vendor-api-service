package com.nextgen.gameaggregator.vendor.pragmaticplay.api.balance;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class WalletBalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = Endpoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.logRequest(request);
        WalletBalanceVo responseVo = new WalletBalanceVo();
        String traceId = UUID.randomUUID().toString();

        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            WalletBalanceDto dto = HttpService.convertQueryStringToDto(body, WalletBalanceDto.class);

            // 1. Validate request parameters from vendor
            ValidationUtils.validateRequest(dto);

            // 2. Verify session token
            // Need to retrieve line credentials from game session in order to validate hash
            // If Token has been tampered, then AuthenticationException will be thrown
            GameSession session = gameSessionService.verifyToken(dto.getToken());

            // 3. Retrieve vendor line credentials and secretKey for hash validation
            String secretKey = vendorLineService.getCredentialValueByName(session.getVendorLineId(), Credentials.SECRET_KEY);

            // 4. Validate request signature
            VendorService.validateHash(body, secretKey);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId);

            responseVo.setCurrency(session.getCurrencyCode()); // TODO: vendor currency code
            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setError(ResponseCodes.INVALID_REQUEST);
            httpRequestLog.setStackTrace(invalidRequestException.getValidation().toString());

        } catch (AuthenticationException authenticationException) {
            responseVo.setError(ResponseCodes.AUTHENTICATION_ERROR);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setError(ResponseCodes.INVALID_HASH);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpRequestLog.setStackTrace(credentialNotFoundException.getMessage());

        } catch (Exception exception) { // any other exception encountered
            responseVo.setError(ResponseCodes.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpRequestLog.setStackTrace(HttpService.getStackTrace(exception));

        } finally {
            responseVo.setDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(responseVo.getError()));
            if (!responseVo.getError().equals(ResponseCodes.SUCCESS)) {
                httpRequestLog.setStatus(1);
            }
            httpService.logResponse(httpRequestLog, responseVo, traceId);
        }

        return responseVo;
    }
}
