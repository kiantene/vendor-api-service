package com.nextgen.gameaggregator.vendor.pragmaticplay.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
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

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
@Slf4j
public class BalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;

    @PostMapping(path = Endpoints.BALANCE)
    public ResponseVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        BalanceVo responseVo = new BalanceVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BalanceDto dto = HttpService.convertQueryStringToDto(body, BalanceDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 5. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setCash(balance);
            responseVo.setBonus(BigDecimal.ZERO);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            if (invalidRequestException.getValidation() != null) {
                httpRequestLog.setErrorMessage(invalidRequestException.getValidation().toString());
            }
            httpService.logError(httpRequestLog, invalidRequestException);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
            httpService.logError(httpRequestLog, invalidPlayerException);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.AUTHENTICATION_ERROR);
            httpService.logError(httpRequestLog, authenticationException);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.INVALID_HASH);
            httpService.logError(httpRequestLog, invalidSignatureException);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, credentialNotFoundException);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_RETRY);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);

        } catch (InvalidAgentApiCredentialException e) {
            responseVo.setResponseCode(ResponseCode.PLAYER_FROZEN);
            httpService.logError(httpRequestLog, e);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            //TODO to be discuss the response code
            responseVo.setResponseCode(ResponseCode.PLAYER_FROZEN);
            httpService.logError(httpRequestLog, disabledVendorLineException);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCode.PLAYER_FROZEN);
            httpService.logError(httpRequestLog, disabledAgentPlayerException);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.INTERNAL_SERVER_ERROR_NO_RETRY);
            httpService.logError(httpRequestLog, exception);
        }

        httpService.end(httpRequestLog, responseVo);
        return responseVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException, InvalidPlayerException {
        // General validation
        ValidationUtils.validateRequest(dto);
        // Validation with custom exception
        ValidationUtils.validateLength(dto.getUserId(), 3, 20, InvalidPlayerException::new);
        //TODO (by Alex), get the provider ID from vendor_line_credentials tables
        ValidationUtils.isEquals(dto.getProviderId(), Credentials.PROVIDER_ID);
    }

    private void doVerification(HttpRequestLog request, BalanceDto dto, GameSession gameSession) throws
            InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException, DisabledAgentPlayerException,
            DisabledVendorLineException {
        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Retrieve vendor line credentials and secretKey for hash validation
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);

        // 4. Verify request signature is valid
        VendorService.verifyHash(request.getRequestBody(), secretKey);

        // 4. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }
}
