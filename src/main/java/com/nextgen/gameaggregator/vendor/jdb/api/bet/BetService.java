package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BetService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;

    public CommonVo bet(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BetDto betDto = HttpService.convertJsonToDto(actionDto.getParams(), BetDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getUid());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 4.3 Process Bet Request
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, actionDto.getParams(), actionDto.getHttpRequestLog());

            vo.setBalance(betEvent.getLastBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            vo.setBalance(betResultIdempotentViolationException.getBalance());
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException authenticationException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);   

        } catch (InsufficientBalanceException nsufficientBalanceException) {
            vo.setErrorResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (InvalidAgentApiCredentialException | GameNotSupportedException | CurrencyNotSupportedException | 
            JsonProcessingException invalidValidRequestException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

        } catch (InvalidRequestException invalidRequestException) {
            if (invalidRequestException.getValidation() != null && !invalidRequestException.getValidation().isEmpty()) {
                String violation = invalidRequestException.getValidation().entrySet().iterator().next().getValue();
                vo.setErrorResponseCode(violation);
            } else {
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
            }

        } catch (DisabledVendorLineException | DisabledGameException | DisabledAgentPlayerException failedException) {
            vo.setErrorResponseCode(ResponseCode.FAILED);

        } catch (TransactionStillProcessingException | InvalidOperatorResponseException cannotCancelException) {
            vo.setErrorResponseCode(ResponseCode.WORK_IN_PROCESS);

        } catch (InvalidPlayerException invalidPlayerException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (Exception exception) {
            vo.setErrorResponseCode(ResponseCode.FAILED);
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

        // Verify vendor gameCode, currency and platform
        String[] parts = gameSession.getVendorGameCode().split("_");
        int mType = Integer.parseInt(parts[1]);
        ValidationUtils.isEquals(String.valueOf(mType), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
