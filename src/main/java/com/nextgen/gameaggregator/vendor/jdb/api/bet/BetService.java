package com.nextgen.gameaggregator.vendor.jdb.api.bet;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.RawGameSession;
import com.nextgen.gameaggregator.eventing.events.UnsettledBetEvent;
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
            RawGameSession rawGameSession = gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(betDto.getUid(), betDto.getMType().toString());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, rawGameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 4.3 Process Bet Request
            //BetEvent betEvent = walletService.processBet(traceId, rawGameSession, betDto, actionDto.getParams());
            UnsettledBetEvent betEvent = walletService.processUnsettledBet(traceId, rawGameSession, betDto, actionDto.getParams());

            vo.setBalance(betEvent.getLastBalance());
            vo.setResponseCode(ResponseCode.SUCCESS);
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);   
        } catch (AuthenticationException authenticationException) {
            vo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);   
        } catch (InsufficientBalanceException nsufficientBalanceException) {
            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
        } catch (CouchbaseDataIntegrityException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (InvalidOperatorResponseException exception) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidAgentApiCredentialException exception) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidRequestException invalidRequestException) {
            if (invalidRequestException.getValidation() != null) {
                String violation = invalidRequestException.getValidation()
                        .entrySet()
                        .stream()
                        .findFirst()
                        .map(Map.Entry::getValue) // get the value of the first element
                        .orElse(ResponseCode.INVALID_REQUEST_PARAMETER); // if there's no value, set it to the default invalid request parameter
                vo.setResponseCode(violation);
            } else {
                vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
            }
        } catch (DisabledVendorLineException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (GameNotSupportedException gameNotSupportedException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            vo.setResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);
        } catch (InvalidPlayerException invalidPlayerException) {
            vo.setResponseCode(ResponseCode.PLAYER_NOT_FOUND);
        } catch (DisabledAgentPlayerException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (DisabledGameException exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        } catch (Exception exception) {
            vo.setResponseCode(ResponseCode.FAILED);
        }

        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
       // General validation
       ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, RawGameSession rawGameSession) throws DisabledVendorLineException,
    DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
    InvalidPlayerException{
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateIllegibleBet(rawGameSession, dto.getUid());

        // Verify vendor gameCode, currency and platform
        ValidationUtils.isEquals(rawGameSession.getVendorGameCode(), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(rawGameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }
}
