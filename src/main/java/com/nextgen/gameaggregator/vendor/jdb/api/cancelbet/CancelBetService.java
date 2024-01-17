package com.nextgen.gameaggregator.vendor.jdb.api.cancelbet;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.service.VendorService;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;

@Service
public class CancelBetService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorService vendorService;

    public CommonVo cancelBet(ActionDto actionDto, String traceId) {
        // Construct VO
        CommonVo vo = new CommonVo();
    
        BigDecimal balance = null;
    
        try {
            // Convert original request body into dto
            CancelBetDto cancelBetDto = HttpService.convertJsonToDto(actionDto.getParams(), CancelBetDto.class);
    
            // 1. Validate request parameters from vendor
            this.doValidation(cancelBetDto);
    
            // 2. Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(cancelBetDto.getUid());
    
            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(cancelBetDto, gameSession);
    
            // 4. Send refund to Operator
            balance = walletService.processRollback(traceId, cancelBetDto, gameSession, vendorService, actionDto.getHttpRequestLog());
            
            vo.setBalance(balance);
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (BetRefundIdempotentViolationException | RecordNotFoundException successException) {
            vo.setSuccessResponseCode(ResponseCode.SUCCESS);

        } catch (AuthenticationException | InvalidPlayerException playerNotFoundException) {
            vo.setErrorResponseCode(ResponseCode.PLAYER_NOT_FOUND);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            vo.setErrorResponseCode(ResponseCode.NO_AUTHORIZED);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
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
            if (invalidRequestException.getValidation() != null) {
                String violation = invalidRequestException.getValidation()
                        .entrySet().stream().findFirst().map(Map.Entry::getValue).orElse(ResponseCode.INVALID_REQUEST_PARAMETER);
                vo.setErrorResponseCode(violation);

            } else {
                vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

            }
        } catch (JsonProcessingException jsonProcessingException) {
            vo.setErrorResponseCode(ResponseCode.INVALID_REQUEST_PARAMETER);

        } catch (DisabledAgentPlayerException | DisabledVendorLineException | CurrencyNotSupportedException | DisabledGameException exception) {
            vo.setErrorResponseCode(ResponseCode.FAILED);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                //if found the bet in settled status
                vo.setErrorResponseCode(ResponseCode.CANNOT_CANCEL);

            } else {
                //if found the bet other in settled status (cancel / refund)
                vo.setBalance(betResultIdempotentViolationException.getBalance());
                vo.setSuccessResponseCode(ResponseCode.SUCCESS);

            }
        } catch (Exception exception) {
            vo.setErrorResponseCode(ResponseCode.FAILED);

        }
    
        return vo;
    }
    
    private void doValidation(CancelBetDto dto) throws InvalidRequestException {
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(CancelBetDto dto, GameSession gameSession) throws DisabledVendorLineException, DisabledAgentPlayerException,
            CurrencyNotSupportedException, InvalidPlayerException, DisabledGameException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUid());

        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);
    }
}
