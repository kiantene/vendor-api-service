package com.nextgen.gameaggregator.vendor.superbullgaming.api.betNSettle;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.superbullgaming.api.promo.SBGPromoPayoutHandler;
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
import com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = Endpoints.PATH)
@Slf4j
public class BetNSettleAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private SBGPromoPayoutHandler promoPayoutHandler;
    @Autowired
    private RequestIdempotentLogService requestIdempotentLogService;

    @PostMapping(path = Endpoints.BET_N_SETTLE)
    public CommonVo betResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CommonVo responseVo = new CommonVo();
        String traceId = httpRequestLog.getId();
        String username = "";
        String vendorCurrencyCode = "";
        GameSession gameSession = null;
        BetNSettleDto dto = new BetNSettleDto();
        boolean isRequestExists = false;

        try {
            // 1. Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            dto = HttpService.convertJsonToDto(body, BetNSettleDto.class);
            username = dto.getUsername();
            vendorCurrencyCode = dto.getCurrency();

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            if (requestIdempotentLogService.checkExists(dto, dto.getUsername()) == null) {
                requestIdempotentLogService.create(dto, dto.getUsername());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 3. Verify session token
            try { //this check only verify if it's null, not status = 0
                gameSession = gameSessionService.verifyToken(dto.getToken());
            } catch (AuthenticationException authenticationException) { //if session expired
                if (dto.getStake().equals(BigDecimal.ZERO)) {
                    gameSession = gameSessionService.generateNewSessionToken(dto.getUsername()); //generate new token
                    gameSessionService.updateByVendorGameCode(gameSession, dto.getGameCode());
                    gameSessionService.updateByVendorCurrencyId(gameSession);
                    gameSession.setToken(traceId);
                    gameSession.setVendorToken(traceId);
                } else {
                    throw new AuthenticationException();
                }
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(httpRequestLog, dto, gameSession);

            // call promo service here
            if (promoPayoutHandler.isPromoPayout(dto)){
                return promoPayoutHandler.process(dto);
            }

            // 5. Send win result to Operator
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(), dto.getJackpotAmount(), true);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            // 6. Set response data
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setBalance(balance);
            responseVo.setUsername(username);
            responseVo.setCurrency(vendorCurrencyCode);
            responseVo.setTimestamp(System.currentTimeMillis());

        } catch (BetResultIdempotentViolationException idempotentViolationException) {
            httpService.logError(httpRequestLog, idempotentViolationException);
            // Return original result when idempotent
            responseVo.setResponseCode(ResponseCode.SUCCESS);
            responseVo.setBalance(idempotentViolationException.getBalance());
            responseVo.setUsername(username);
            responseVo.setCurrency(vendorCurrencyCode);
            responseVo.setTimestamp(System.currentTimeMillis());

        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            responseVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            httpService.logError(httpRequestLog, credentialNotFoundException);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

        } catch (InvalidPlayerException invalidPlayerException) {
            httpService.logError(httpRequestLog, invalidPlayerException);
            responseVo.setResponseCode(ResponseCode.INVALID_PLAYER);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            responseVo.setResponseCode(ResponseCode.INVALID_TOKEN);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            if (invalidOperatorResponseException.getOperatorStatus().equals(Status.SC_INSUFFICIENT_FUNDS.code)) {
                responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            } else {   
                responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);
            }

        } catch (InvalidSignatureException invalidSignatureException) {
            httpService.logError(httpRequestLog, invalidSignatureException);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            responseVo.setResponseCode(ResponseCode.BET_NOT_FOUND);
            httpRequestLog.setErrorMessage(betNotFoundException.getMessage());

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            httpService.logError(httpRequestLog, disabledAgentPlayerException);
            responseVo.setResponseCode(ResponseCode.INACTIVE_PLAYER);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            httpService.logError(httpRequestLog, disabledVendorLineException);
            responseVo.setResponseCode(ResponseCode.OPERATION_FAILED);

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            responseVo.setResponseCode(ResponseCode.GAME_DOES_NOT_EXIST);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            responseVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            httpService.logError(httpRequestLog, transactionStillProcessingException);
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

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(HttpRequestLog request, BetNSettleDto dto, GameSession gameSession)
            throws InvalidPlayerException, CredentialNotFoundException, InvalidSignatureException,
            AuthenticationException, DisabledAgentPlayerException, DisabledVendorLineException, DisabledGameException {

        if (dto.getStake().compareTo(BigDecimal.ZERO) > 0) {
            validationService.validateEligibleBet(gameSession, dto.getUsername());
        }

        // Verify operator ID
        ValidationUtils.isEquals(vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), "operator"), dto.getOperatorId(), CredentialNotFoundException::new);
    }
}
