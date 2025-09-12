package com.nextgen.gameaggregator.vendor.jdb.api.endround;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BetNSettleService {

    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    public BetNSettleService(GameServiceImpl gameService,
                             WalletService walletService,
                             ValidationService validationService,
                             VendorService vendorService,
                             HttpService httpService,
                             RequestIdempotentLogService requestIdempotentLogService) {

        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo betNSettle(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        // Construct VO
        CommonVo vo = new CommonVo();
        boolean isRequestExists = false;
        BetNSettleDto betNSettleDto = new BetNSettleDto();

        try {
            // Convert original request body into dto
            betNSettleDto = HttpService.convertJsonToDto(actionDto.getParams(), BetNSettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(betNSettleDto);

            // 2. Verify session token
            GameSession gameSession = gameService.getGameSessionByUsername(betNSettleDto.getUid(), betNSettleDto.getGType() + "_" + betNSettleDto.getMType());

            // 3. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(betNSettleDto, betNSettleDto.getUid()) == null) {
                requestIdempotentLogService.create(betNSettleDto, betNSettleDto.getUid());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(betNSettleDto, gameSession);

            // 5. Send bet request to Operator
            // 5.1 check if player has enough balance
            // 5.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 5.3 Process Bet Result and End Round
            ResultType resultType = getResultType(betNSettleDto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betNSettleDto, resultType, vendorService, actionDto.getHttpRequestLog());

            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            vo.setBalance(betResultIdempotentViolationException.getBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException | InvalidPlayerException playerNotFoundException) {
            httpService.logError(httpRequestLog, playerNotFoundException);
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (BetNotFoundException | DisabledAgentPlayerException | DisabledVendorLineException |
                 DisabledGameException failedException) {
            httpService.logError(httpRequestLog, failedException);
            vo.setErrorResponseCode(ResponseCode.FAILED);

        } catch (TransactionStillProcessingException cannotCancelException) {
            httpService.logError(httpRequestLog, cannotCancelException);
            vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                //insufficient balance
                vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

            } else {
                //Other operator errors
                vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

            }
        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);

        } catch (JsonProcessingException |
                 CurrencyNotSupportedException invalidValidRequestException) {
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

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            vo.setErrorResponseCode(ResponseCode.FAILED);
        } finally {
            // first request (not request exist) will delete log after process finish.
            if (!isRequestExists) {
                requestIdempotentLogService.delete(betNSettleDto, betNSettleDto.getUid());
            }
        }

        return vo;
    }

    private void doValidation(BetNSettleDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetNSettleDto dto, GameSession gameSession) throws DisabledAgentPlayerException,
            DisabledVendorLineException, DisabledGameException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUid());

        // Verify currency and platform
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }

    private ResultType getResultType(BetNSettleDto dto) {

        ResultType resultType = ResultType.BET_LOSE;
        BigDecimal zero = BigDecimal.ZERO;

        if (dto.getWinAmount().compareTo(zero) > 0 || dto.getJackpotAmount().compareTo(zero) > 0) {
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }
}
