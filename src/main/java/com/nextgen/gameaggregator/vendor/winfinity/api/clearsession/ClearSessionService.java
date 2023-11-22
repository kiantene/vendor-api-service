package com.nextgen.gameaggregator.vendor.winfinity.api.clearsession;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;

@Service
public class ClearSessionService {
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private HttpService httpService;

    public ResponseVo clearSession(String traceId, String body, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();

        try {
            // Convert original request body into dto
            ClearSessionDto dto = HttpService.convertJsonToDto(body, ClearSessionDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession with token
            GameSession gameSession = gameSessionService.verifyToken(dto.getMsid());

            if (gameSession.getStatus() != 0) {
                BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
                vo.setDataVo(traceId, balance);

                // Clear GameSession
                gameSessionService.clearGameSession(gameSession, gameSession.getAgentPlayerUsername(), gameSession.getVendorGameCode());

            } else {
                throw new AuthenticationException();
            }

        } catch (JsonProcessingException jsonProcessingException) {
            httpService.logError(httpRequestLog, jsonProcessingException);
            vo.setErrorVo(ErrorCodes.BAD_REQUEST);

        } catch (AuthenticationException authenticationException) {
            httpService.logError(httpRequestLog, authenticationException);
            vo.setErrorVo(ErrorCodes.WRONG_SESSION);

        } catch (InvalidOperatorResponseException | InvalidAgentApiCredentialException unknownErrorException) {
            httpService.logError(httpRequestLog, unknownErrorException);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);

        } catch (Exception exception) { // Any other exception encountered
            httpService.logError(httpRequestLog, exception);
            vo.setErrorVo(ErrorCodes.UNKNOWN_ERROR);
        }

        return vo;
    }

    private void doValidation(ClearSessionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
}
