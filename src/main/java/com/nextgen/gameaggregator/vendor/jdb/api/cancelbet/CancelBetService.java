package com.nextgen.gameaggregator.vendor.jdb.api.cancelbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class CancelBetService {
    private final GameService gameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public CancelBetService(GameServiceImpl gameService,
                            GameSessionService gameSessionService,
                            WalletService walletService,
                            VendorService vendorService,
                            HttpService httpService,
                            RequestIdempotentLogService requestIdempotentLogService) {

        this.gameService = gameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo cancelBet(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        // Construct VO
        CommonVo vo = new CommonVo();
        boolean isRequestExists = false;
        CancelBetDto cancelBetDto = new CancelBetDto();
        BigDecimal balance = null;

        try {
            // Convert original request body into dto
            cancelBetDto = HttpService.convertJsonToDto(actionDto.getParams(), CancelBetDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(cancelBetDto);

            // 2. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(cancelBetDto, cancelBetDto.getUid()) == null) {
                requestIdempotentLogService.create(cancelBetDto, cancelBetDto.getUid());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 3. Verify session token
            GameSession gameSession;
            try {
                gameSession = gameService.getGameSessionByUsername(cancelBetDto.getUid(), cancelBetDto.getGType() + "_" + cancelBetDto.getMType()); //token check
            } catch (AuthenticationException authenticationException) { //if expired
                gameSession = gameSessionService.generateNewSessionToken(cancelBetDto.getUid()); //generate new token
                gameSessionService.updateByVendorCurrencyCode(gameSession, cancelBetDto.getCurrency());
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetDto, gameSession);

            // 5. Send refund to Operator
            balance = walletService.processRollback(traceId, cancelBetDto, gameSession, vendorService, actionDto.getHttpRequestLog());

            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (BetRefundIdempotentViolationException | RecordNotFoundException successException) {
            httpService.logError(httpRequestLog, successException);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                //insufficient balance
                vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

            } else if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                //Operator Bet not found
                vo.setErrorResponseCode(ResponseCode.FAILED);

            } else {
                //Other operator errors
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

            }
        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            if (invalidRequestException.getValidation() != null) {
                String violation = invalidRequestException.getValidation()
                        .entrySet().stream().findFirst().map(Map.Entry::getValue).orElse(ResponseCode.INVALID_REQUEST_PARAMETER);
                vo.setErrorResponseCode(violation);

            } else {
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

            }
        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                //if found the bet in settled status
                vo.setErrorResponseCode(ResponseCode.CANNOT_CANCEL);

            } else {
                //if found the bet other in settled status (cancel / refund)
                vo.setBalance(betResultIdempotentViolationException.getBalance());
                vo.setSuccessResponseCode(ResponseCode.SUCCESS);

            }
        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.FAILED);

        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(cancelBetDto, cancelBetDto.getUid());
            }
        }

        return vo;
    }

    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetDto dto, GameSession gameSession) throws CurrencyNotSupportedException {
        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
