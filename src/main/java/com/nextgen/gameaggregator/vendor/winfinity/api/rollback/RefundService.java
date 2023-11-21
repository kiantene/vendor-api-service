package com.nextgen.gameaggregator.vendor.winfinity.api.rollback;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.mg.service.VendorService;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefundService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;

    public ResponseVo refund(String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        try {
            // Convert original request body into dto
            RefundDto dto = HttpService.convertJsonToDto(body, RefundDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getUid());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(dto, gameSession);
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);
            
            vo.setDataVo(traceId, balance);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorVo(ErrorCodes.WRONG_SESSION);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
            if (invalidOperatorResponseException.getOperatorStatus() == 11) {
                // Insufficient balance
                vo.setErrorVo(ErrorCodes.NOT_ENOUGH_FUND);

            } else if (invalidOperatorResponseException.getOperatorStatus() == 15) {
                // Operator bet not found
                vo.setErrorVo(ErrorCodes.PAYIN_TRANS_NOT_FOUND);

            } else {
                // Other operator errors
                vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
            }
   
        } catch (InvalidAgentApiCredentialException | TransactionStillProcessingException | DisabledVendorLineException | 
            DisabledAgentPlayerException unknownErrorException) {
            httpService.logError(httpRequestLog, unknownErrorException);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
        
        } catch (JsonProcessingException | InvalidRequestException badRequestException) {
            httpService.logError(httpRequestLog, badRequestException);
            vo.setErrorVo(ErrorCodes.BAD_REQUEST);

        } catch (RecordNotFoundException recordNotFoundException) {
            httpService.logError(httpRequestLog, recordNotFoundException);
            vo.setErrorVo(ErrorCodes.PAYIN_TRANS_NOT_FOUND);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            if (betResultIdempotentViolationException.getStatus() == BetStatus.SETTLED.code) {
                // If found the bet in settled status
                vo.setErrorVo(ErrorCodes.TRANS_REFUNDED);

            } else {
                // If found the bet other in settled status (cancel / refund)
                vo.setDataVo(traceId, betResultIdempotentViolationException.getBalance());
            }

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            httpService.logError(httpRequestLog, betRefundIdempotentViolationException);
            vo.setErrorVo(ErrorCodes.TRANS_REFUNDED);

        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            vo.setErrorVo(ErrorCodes.PAYIN_TRANS_NOT_FOUND);

        } catch (DisabledGameException disabledGameException) {
            httpService.logError(httpRequestLog, disabledGameException);
            vo.setErrorVo(ErrorCodes.GAME_NOT_AVAILABLE);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            httpService.logError(httpRequestLog, currencyNotSupportedException);
            vo.setErrorVo(ErrorCodes.CURRENCY_NOT_ALLOWED);

        } catch (InvalidPlayerException invalidPlayerException) {
            httpService.logError(httpRequestLog, invalidPlayerException);
            vo.setErrorVo(ErrorCodes.PLAYER_NOT_ALLOWED);
            
        } catch (Exception exception) { // Any other exception encountered
            httpService.logError(httpRequestLog, exception);
            log.error("Other exception encountered: " + exception.getMessage());
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
        }

        return vo;
    }

    private void doValidation(RefundDto dto) throws InvalidRequestException {
       // General validation
       ValidationUtils.validateRequest(dto);
    }

    private void doVerification(RefundDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUid());
    }
}
