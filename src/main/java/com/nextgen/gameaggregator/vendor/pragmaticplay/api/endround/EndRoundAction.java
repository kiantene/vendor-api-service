package com.nextgen.gameaggregator.vendor.pragmaticplay.api.endround;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
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
public class EndRoundAction {
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

    @PostMapping(path = Endpoints.END_ROUND)
    public ResponseVo endRound(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        EndRoundVo responseVo = new EndRoundVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            EndRoundDto dto = HttpService.convertQueryStringToDto(httpRequestLog, EndRoundDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getGameId(), gameSession);

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 4. Retrieve the bet transaction
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, ResultType.END, vendorService, httpRequestLog);

            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_END_ROUND_RETRY);
            httpService.logError(httpRequestLog, transactionStillProcessingException);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            responseVo.setCash(betResultIdempotentViolationException.getBalance());
            responseVo.setBonus(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, credentialNotFoundException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_END_ROUND_RETRY);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);
            httpService.logError(httpRequestLog, invalidSignatureException);

        } catch (BetNotFoundException betNotFoundException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());
            httpService.logError(httpRequestLog, betNotFoundException);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            //Set balance to zero if agent credential is disabled
            responseVo.setCash(BigDecimal.ZERO);
            responseVo.setBonus(BigDecimal.ZERO);
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);

        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            responseVo.setResponseCode(ResponseCode.BET_NOT_ALLOWED);
            httpService.logError(httpRequestLog, mergedBetDataIntegrityException);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidation(EndRoundDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        //TODO (by Alex), get the provider ID from vendor_line_credentials tables
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, EndRoundDto dto, GameSession gameSession) throws
            InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidSignatureException {
        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // 2. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 3. Validate request signature
        VendorService.verifyHash(request.getRequestBody(), secretKey);
    }
}
