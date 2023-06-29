package com.nextgen.gameaggregator.vendor.alize.api.bet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.alize.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.alize.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.alize.vo.CommonVo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = Endpoints.PATH)
@Slf4j
public class BetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    @PostMapping(path = Endpoints.PLACE_BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        String traceId = httpRequestLog.getId();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BetDto dto = HttpService.convertJsonToDto(body, BetDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUsername());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 5. Send bet request to Operator
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);

            // 6. Set response data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setBalance(betEvent.getLastBalance());
            responseVo.setUsername(dto.getUsername());
            responseVo.setCurrency(dto.getCurrency());
            responseVo.setTimestamp(System.currentTimeMillis());

        } catch (JsonProcessingException jsonProcessingException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (AuthenticationException authenticationException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidPlayerException invalidPlayerException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (InvalidSignatureException invalidSignatureException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (DisabledGameException disabledGameException) {
            responseVo.setResponseCode(ResponseCode.ERROR);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, exception);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, BetDto dto, GameSession gameSession)
            throws InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException,
            AuthenticationException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException {

        validationService.validateEligibleBet(gameSession, dto.getUsername());
    }
}
