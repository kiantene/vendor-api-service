package com.nextgen.gameaggregator.vendor.winfinity.api.rollback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.mg.service.VendorService;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RefundService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private HttpService httpService;

    public ResponseVo refund(String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        try {
            // Convert original request body into dto
            RefundDto dto = HttpService.convertJsonToDto(body, RefundDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession with token
            GameSession gameSession = gameSessionService.verifyToken(dto.getMsid());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getTbid(), gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession);
            
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

        } catch (InvalidAgentApiCredentialException | TransactionStillProcessingException unknownErrorException) {
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
            vo.setDataVo(traceId, betResultIdempotentViolationException.getBalance());

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            httpService.logError(httpRequestLog, betRefundIdempotentViolationException);
            vo.setErrorVo(ErrorCodes.TRANS_REFUNDED);

        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            vo.setErrorVo(ErrorCodes.PAYIN_TRANS_NOT_FOUND);

        } catch (Exception exception) { // Any other exception encountered
            httpService.logError(httpRequestLog, exception);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
        }

        return vo;
    }

    private void doValidation(RefundDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession) throws AuthenticationException {
        
        if (gameSession.getStatus() == 0) throw new AuthenticationException();
    }
}
