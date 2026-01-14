package com.nextgen.gameaggregator.vendor.superbullgaming.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.superbullgaming.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.superbullgaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.superbullgaming.service.VendorService;
import com.nextgen.gameaggregator.vendor.superbullgaming.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH)
@Slf4j
public class CancelBetAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private RequestIdempotentLogService requestIdempotentLogService;

    @PostMapping(path = Endpoints.CANCEL_BET)
    public CommonVo cancelBet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        String traceId = httpRequestLog.getId();
        boolean isRequestExists = false;
        CancelBetDto dto = new CancelBetDto();

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            dto = HttpService.convertJsonToDto(body, CancelBetDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            if (requestIdempotentLogService.checkExists(dto, dto.getUsername()) == null) {
                requestIdempotentLogService.create(dto, dto.getUsername());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 3. Verify session token
            GameSession gameSession;
            try { //this check only verify if it's null, not status = 0
                gameSession = gameSessionService.verifyToken(dto.getToken());
            } catch (AuthenticationException authenticationException) { //if session expired
                gameSession = gameSessionService.generateNewSessionToken(dto.getUsername()); //generate new token
                gameSessionService.updateByVendorGameCode(gameSession, dto.getGameCode());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 5. Process rollback
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            // 6. Set response data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setBalance(balance);
            responseVo.setUsername(dto.getUsername());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setTimestamp(System.currentTimeMillis());

        } catch (JsonProcessingException | InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INVALID_PLAYER);

        } catch (CredentialNotFoundException | InvalidSignatureException | InvalidAgentApiCredentialException | InvalidOperatorResponseException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INVALID_TOKEN);

        } catch (DisabledAgentPlayerException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INACTIVE_PLAYER);

        } catch (DisabledVendorLineException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

        } catch (RecordNotFoundException | BetNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.BET_NOT_FOUND);

        } catch (BetRefundIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.PLAYERS_OPERATION_IN_PROGRESS);

        } catch (DisabledGameException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.GAME_DOES_NOT_EXIST);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            httpService.logError(httpRequestLog, transactionStillProcessingException);
            responseVo.setResponseCode(ResponseCode.PLAYERS_OPERATION_IN_PROGRESS);

        } catch (Exception e) { // any other exception encountered
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getUsername());
            }
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, CancelBetDto dto, GameSession gameSession)
            throws InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException,
            AuthenticationException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException {

        // Verify operator ID
        ValidationUtils.isEquals(vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator"), dto.getOperatorId(), CredentialNotFoundException::new);
    }
}
