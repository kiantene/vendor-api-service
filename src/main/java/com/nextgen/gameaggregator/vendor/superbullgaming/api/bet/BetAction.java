package com.nextgen.gameaggregator.vendor.superbullgaming.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.superbullgaming.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.superbullgaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.superbullgaming.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private RequestIdempotentLogService requestIdempotentLogService;

    @PostMapping(path = Endpoints.PLACE_BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        String traceId = httpRequestLog.getId();
        BetDto dto = new BetDto();
        boolean isRequestExists = false;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            dto = HttpService.convertJsonToDto(body, BetDto.class);

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(dto, dto.getUsername()) == null) {
                requestIdempotentLogService.create(dto, dto.getUsername());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 4. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 5. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // 6. Send bet request to Operator
            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);

            // 7. Set response data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setBalance(betEvent.getLastBalance());
            responseVo.setUsername(dto.getUsername());
            responseVo.setCurrency(dto.getCurrency());
            responseVo.setTimestamp(System.currentTimeMillis());

        } catch (JsonProcessingException | InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (AuthenticationException | InvalidSignatureException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INVALID_TOKEN);

        } catch (InvalidPlayerException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INVALID_PLAYER);

        } catch (DisabledAgentPlayerException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INACTIVE_PLAYER);

        } catch (DisabledGameException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.GAME_DOES_NOT_EXIST);

        } catch (CredentialNotFoundException | InvalidAgentApiCredentialException | DisabledVendorLineException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

         } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            if (invalidOperatorResponseException.getOperatorStatus().equals(Status.SC_INSUFFICIENT_FUNDS.code)) {
                responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            } else {   
                responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);
            }

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setResponseCode(ResponseCode.PLAYERS_OPERATION_IN_PROGRESS);

        } catch (Exception exception) { // any other exception encountered
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);
            httpService.logError(httpRequestLog, exception);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(dto, dto.getUsername());
            }
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
        // Verify operator ID
        ValidationUtils.isEquals(vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator"), dto.getOperatorId(), CredentialNotFoundException::new);
    }
}
