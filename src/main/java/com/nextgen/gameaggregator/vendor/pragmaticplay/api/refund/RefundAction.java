package com.nextgen.gameaggregator.vendor.pragmaticplay.api.refund;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.RawBetRefundLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Credentials;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pragmaticplay.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pragmaticplay.service.VendorService;
import com.nextgen.gameaggregator.vendor.pragmaticplay.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class RefundAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = Endpoints.REFUND)
    public ResponseVo refund(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        RefundVo responseVo = new RefundVo();
        String traceId = httpRequestLog.getId();
        String transactionId = null;

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            RefundDto dto = HttpService.convertQueryStringToDto(body, RefundDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Send refund to Operator
            walletService.processRollback(traceId, dto, gameSession, vendorService);

            transactionId = VendorService.getTransactionId(traceId);
            responseVo.setTransactionId(transactionId);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                //if found the bet in settled status
                responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);

            } else {
                //if found the bet other in settled status (cancel / refund)
                transactionId = betResultIdempotentViolationException.getTransactionId();
                responseVo.setTransactionId(transactionId);

            }

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (
                InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                //Operator Bet not found
                responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            } else {
                //Other operator errors
                responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            }

            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (
                InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                String validations = invalidRequestException.getValidation().toString();
                log.error(validations);
                httpRequestLog.setErrorMessage(validations);
            }

        } catch (
                InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (
                AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);

        } catch (
                InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);

        } catch (
                CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (
                InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);

        } catch (
                Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidation(RefundDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        //TODO (by Alex), get the provider ID from vendor_line_credentials tables
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, RefundDto dto, GameSession gameSession) throws InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException {
        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // 2. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 3. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);

    }
}
