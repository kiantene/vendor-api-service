package com.nextgen.gameaggregator.vendor.evolution.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.evolution.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.evolution.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.evolution.dto.BasicDto;
import com.nextgen.gameaggregator.vendor.evolution.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BalanceAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.BALANCE)
    public ResponseVo BalanceAction(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        ResponseVo responseVo = new ResponseVo();
        String traceId = httpRequestLog.getId();

        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(balanceDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(balanceDto.getSid());

            // Check if token status 0 then show invalid sid
            if (gameSession.getStatus() == 0) {
                throw new AuthenticationException();
            }

            this.doVerification(balanceDto, gameSession);

            // 3. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setBalance(balance);
            responseVo.setUuid(balanceDto.getUuid());

        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_SID);

        } catch (JsonProcessingException |
                 InvalidRequestException |
                 CurrencyNotSupportedException |
                 InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.INVALID_PARAMETER);

        } catch (DisabledVendorLineException |
                 DisabledGameException |
                 InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException e) {
            responseVo.setResponseCode(ResponseCode.TEMPORARY_ERROR);

        } catch (DisabledAgentPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ACCOUNT_LOCKED);

        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(BalanceDto balanceDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest((BasicDto) balanceDto);
        ValidationUtils.validateRequest(balanceDto);
    }

    private void doVerification(BalanceDto balanceDto, GameSession gameSession)
            throws AuthenticationException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, CurrencyNotSupportedException, InvalidPlayerException {

        // 1. Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), balanceDto.getSid(), AuthenticationException::new);
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), balanceDto.getUserId(), InvalidPlayerException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), balanceDto.getCurrency(), CurrencyNotSupportedException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }
}
