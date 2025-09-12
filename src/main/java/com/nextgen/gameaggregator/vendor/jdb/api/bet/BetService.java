package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BetService {

    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final HttpService httpService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public BetService(GameServiceImpl gameService,
                      WalletService walletService,
                      ValidationService validationService,
                      HttpService httpService,
                      RequestIdempotentLogService requestIdempotentLogService) {

        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.httpService = httpService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo bet(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        // Construct VO
        CommonVo vo = new CommonVo();
        boolean isRequestExists = false;
        BetDto betDto = new BetDto();

        try {
            // Convert original request body into dto
            betDto = HttpService.convertJsonToDto(actionDto.getParams(), BetDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // 2. Verify session token
            GameSession gameSession = gameService.getGameSessionByUsername(betDto.getUid(), betDto.getGType() + "_" + betDto.getMType());

            // 3. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(betDto, betDto.getUid()) == null) {
                requestIdempotentLogService.create(betDto, betDto.getUid());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            // 5. Send bet request to Operator
            // 5.1 check if player has enough balance
            // 5.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 5.3 Process Bet Request
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, actionDto.getParams(), actionDto.getHttpRequestLog());

            vo.setBalance(betEvent.getLastBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            vo.setBalance(betResultIdempotentViolationException.getBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (InvalidAgentApiCredentialException | GameNotSupportedException | CurrencyNotSupportedException |
                 JsonProcessingException invalidValidRequestException) {
            httpService.logError(httpRequestLog, invalidValidRequestException);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

        } catch (InvalidRequestException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            if (invalidRequestException.getValidation() != null && !invalidRequestException.getValidation().isEmpty()) {
                String violation = invalidRequestException.getValidation().entrySet().iterator().next().getValue();
                vo.setErrorResponseCode(violation);
            } else {
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
            }

        } catch (DisabledVendorLineException | DisabledGameException | DisabledAgentPlayerException failedException) {
            httpService.logError(httpRequestLog, failedException);
            vo.setErrorResponseCode(ResponseCode.FAILED);

        } catch (TransactionStillProcessingException | InvalidOperatorResponseException cannotCancelException) {
            httpService.logError(httpRequestLog, cannotCancelException);
            vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

        } catch (InvalidPlayerException invalidPlayerException) {
            httpService.logError(httpRequestLog, invalidPlayerException);
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(betDto, betDto.getUid());
            }
        }

        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException {

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUid());

        // Verify currency and platform
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
