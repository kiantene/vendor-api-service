package com.nextgen.gameaggregator.vendor.winfinity.api.clearsession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.InvalidAgentApiCredentialException;
import com.nextgen.gameaggregator.exception.InvalidOperatorResponseException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.winfinity.constant.ErrorCodes;
import com.nextgen.gameaggregator.vendor.winfinity.vo.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
        GameSession gameSession;
        BigDecimal balance;

        try {
            // Convert original request body into dto
            ClearSessionDto dto = HttpService.convertJsonToDto(body, ClearSessionDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(dto);

            // Get GameSession with token
            gameSession = gameSessionService.verifyToken(dto.getMsid()); //throw authenticationException
            if (gameSession.getStatus() != 0) {
                balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
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
            // If cause authentication error will return success
            vo.setDataVo(traceId, BigDecimal.ZERO);

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
