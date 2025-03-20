package com.nextgen.gameaggregator.vendor.jdb.api.cancelbetnsettle;

import com.fasterxml.jackson.core.JsonProcessingException;
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

@Service
public class CancelBetNSettleService {

    private final GameService gameService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final HttpService httpService;

    public CancelBetNSettleService(GameServiceImpl gameService,
                                   GameSessionService gameSessionService,
                                   WalletService walletService,
                                   VendorService vendorService,
                                   HttpService httpService) {

        this.gameService = gameService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.httpService = httpService;
    }

    public CommonVo cancelBetNSettle(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            CancelBetNSettleDto cancelBetNSettleDto = HttpService.convertJsonToDto(actionDto.getParams(), CancelBetNSettleDto.class);

            // 1. Validate request parameters from vendor
            this.doValidation(cancelBetNSettleDto);

            // 2. Verify session token
            GameSession gameSession;
            try {
                gameSession = gameService.getGameSessionByUsername(cancelBetNSettleDto.getUid());
            } catch (AuthenticationException playerNotFoundException) {
                gameSession = gameSessionService.generateNewSessionToken(cancelBetNSettleDto.getUid()); //generate new token
                gameSessionService.updateByVendorCurrencyId(gameSession);
                gameSession.setToken(traceId);
                gameSession.setVendorToken(traceId);
            }
            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetNSettleDto, gameSession);

            // 4. Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, cancelBetNSettleDto, gameSession, vendorService, actionDto.getHttpRequestLog());

            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);


        } catch (BetNotFoundException | RecordNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            vo.setErrorResponseCode(ResponseCode.DATA_NOT_EXIST);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            httpService.logError(httpRequestLog, invalidAgentApiCredentialException);
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);

        } catch (InvalidRequestException | JsonProcessingException invalidRequestException) {
            httpService.logError(httpRequestLog, invalidRequestException);
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                //insufficient balance
                vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

            } else if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                //Operator Bet not found
                vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

            } else {
                //Other operator errors
                vo.setErrorResponseCode(ResponseCode.FAILED);

            }
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            httpService.logError(httpRequestLog, transactionStillProcessingException);
            vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

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

        }

        return vo;
    }

    private void doValidation(CancelBetNSettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetNSettleDto dto, GameSession gameSession) throws InvalidPlayerException {
        //validate vendor username, agent vendor line, player status, and game status
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);
    }
}
