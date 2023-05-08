package com.nextgen.gameaggregator.vendor.pragmaticplay.api.promo;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.RawBetResultLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class PromoAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private CachingService cachingService;

    @PostMapping(path = Endpoints.PROMO)
    public ResponseVo betResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        PromoVo responseVo = new PromoVo();
        String traceId = httpRequestLog.getTraceId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            //TODO: refine dto
            PromoDto dto = HttpService.convertQueryStringToDto(body, PromoDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUserId());

            // 3. Verify remaining parameters (Verify against database values)
//            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Send win result to Operator
            BigDecimal balance = walletService.processPromo(traceId, gameSession, dto, body);

            String transactionId = traceId.replace("-", "");

            responseVo.setTransactionId(transactionId);
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (BetResultIdempotentViolationException idempotentViolationException) {
            // duplicate bet result received, do not process but return original transaction id back to vendor
            RawBetResultLog rawBetResultLog = idempotentViolationException.getBetResultLog();
            responseVo.setTransactionId(VendorService.getTransactionId(rawBetResultLog.getResultLogId()));
            responseVo.setCash(rawBetResultLog.getBalance());

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }
//        } catch (CredentialNotFoundException credentialNotFoundException) {
//            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);

//        } catch (AuthenticationException authenticationException) {
//            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);
//
//        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
//            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
//            httpService.logError(httpRequestLog, invalidOperatorResponseException);
//
//        } catch (InvalidSignatureException invalidSignatureException) {
//            responseVo.setResponseCode(ResponseCode.INVALID_HASH);
//
//        } catch (InvalidAgentApiCredentialException InvalidAgentApiCredentialException) {
//            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
//
//        } catch (BetNotFoundException betNotFoundException) {
//            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
//            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidation(PromoDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        //TODO (by Alex), get the provider ID from vendor_line_credentials tables
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, PromoDto dto, GameSession gameSession) throws
            InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException, AuthenticationException {

        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // 3. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 4. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }
}
