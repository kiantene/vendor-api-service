package com.nextgen.gameaggregator.vendor.winfinity.api.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.service.VendorService;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PayoutService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private HttpService httpService;

    public ResponseVo payout(String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        try {
            // Convert original request body into dto
            PayoutDto dto = HttpService.convertJsonToDto(body, PayoutDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession with token
            GameSession gameSession = gameSessionService.verifyToken(dto.getMsid());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getTbid(), gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession);

            // Determine result type
            ResultType resultType = determineResultType(dto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, dto, resultType, vendorService, httpRequestLog);

            vo.setDataVo(traceId, balance);

        } catch (JsonProcessingException | TransactionStillProcessingException | InvalidRequestException badRequestException) {
            httpService.logError(httpRequestLog, badRequestException);
            vo.setErrorVo(ErrorCodes.BAD_REQUEST);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorVo(ErrorCodes.WRONG_SESSION);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            vo.setErrorVo(ErrorCodes.NOT_ENOUGH_FUND);

        } catch (BetNotFoundException betNotFoundException) {
            httpService.logError(httpRequestLog, betNotFoundException);
            vo.setErrorVo(ErrorCodes.PAYIN_TRANS_NOT_FOUND);

        } catch (MergedBetDataIntegrityException | InvalidOperatorResponseException | InvalidAgentApiCredentialException unknownErrorException) {
            httpService.logError(httpRequestLog, unknownErrorException);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);

        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            httpService.logError(httpRequestLog, betResultIdempotentViolationException);
            vo.setDataVo(traceId, betResultIdempotentViolationException.getBalance());

        } catch (Exception exception) { // Any other exception encountered
            httpService.logError(httpRequestLog, exception);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
        }

        return vo;
    }

    private ResultType determineResultType(PayoutDto dto) {
        return dto.getSum().compareTo(BigDecimal.ZERO) > 0 ? ResultType.WIN : ResultType.LOSE;
    }

    private void doValidation(PayoutDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession) throws AuthenticationException {
        
        if (gameSession.getStatus() == 0) throw new AuthenticationException();
    }
}
