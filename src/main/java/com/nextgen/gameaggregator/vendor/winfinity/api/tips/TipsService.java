package com.nextgen.gameaggregator.vendor.winfinity.api.tips;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
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

@Service
public class TipsService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    public ResponseVo tips(String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        try {
            // Convert original request body into dto
            TipsDto dto = HttpService.convertJsonToDto(body, TipsDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession by vendor player username
            GameSession gameSession = gameSessionService.verifyToken(dto.getMsid());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(dto.getTbid(), gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(gameSession);

            BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);

            vo.setDataVo(traceId, betEvent.getLastBalance());

        } catch (JsonProcessingException | TransactionStillProcessingException | InvalidRequestException badRequestException) {
            httpService.logError(httpRequestLog, badRequestException);
            vo.setErrorVo(ErrorCodes.BAD_REQUEST);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorVo(ErrorCodes.WRONG_SESSION);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            httpService.logError(httpRequestLog, insufficientBalanceException);
            vo.setErrorVo(ErrorCodes.NOT_ENOUGH_FUND);

        } catch (InvalidOperatorResponseException | InvalidAgentApiCredentialException unknownErrorException) {
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

    private void doValidation(TipsDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(GameSession gameSession) throws AuthenticationException {
        
        if (gameSession.getStatus() == 0) throw new AuthenticationException();
    }
}
