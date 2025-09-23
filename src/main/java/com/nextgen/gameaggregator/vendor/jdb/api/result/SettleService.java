package com.nextgen.gameaggregator.vendor.jdb.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameService;
import com.nextgen.gameaggregator.service.GameServiceImpl;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
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
public class SettleService {

    private final GameService gameService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final HttpService httpService;
    private final RequestIdempotentLogService requestIdempotentLogService;


    public SettleService(GameServiceImpl gameService,
                         WalletService walletService,
                         VendorService vendorService,
                         HttpService httpService,
                         RequestIdempotentLogService requestIdempotentLogService) {

        this.gameService = gameService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo settle(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        // Construct VO
        CommonVo vo = new CommonVo();
        boolean isRequestExists = false;
        SettleDto settleDto = new SettleDto();

        try {
            // Convert original request body into dto
            settleDto = HttpService.convertJsonToDto(actionDto.getParams(), SettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // 2. Request idempotent checking.
            if (requestIdempotentLogService.checkExists(settleDto, settleDto.getUid()) == null) {
                requestIdempotentLogService.create(settleDto, settleDto.getUid());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }

            // 3. Verify session token
            GameSession gameSession = gameService.getGameSessionByUsername(settleDto.getUid(), settleDto.getGType() + "_" + settleDto.getMType());

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto,
                    (settleDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.END, vendorService, actionDto.getHttpRequestLog());

            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            vo.setBalance(betResultIdempotentViolationException.getBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (BetNotFoundException e) {
            httpService.logError(httpRequestLog, e);
            vo.setErrorResponseCode(ResponseCode.FAILED);

        } catch (TransactionStillProcessingException | InvalidOperatorResponseException cannotCancelException) {
            httpService.logError(httpRequestLog, cannotCancelException);
            vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (InvalidAgentApiCredentialException | JsonProcessingException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
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
                requestIdempotentLogService.delete(settleDto, settleDto.getUid());
            }
        }

        return vo;
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }
}
