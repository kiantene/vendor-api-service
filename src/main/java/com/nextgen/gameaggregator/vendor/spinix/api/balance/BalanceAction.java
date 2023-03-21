package com.nextgen.gameaggregator.vendor.spinix.api.balance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
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
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.BALANCE)
    public BalanceVo balance(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        BalanceVo balanceVo = new BalanceVo();
        BalanceErrorVo balanceErrorVo = new BalanceErrorVo();
        BalanceDataVo balanceDataVo = new BalanceDataVo();
        String traceId = httpRequestLog.getTraceId();
        String body = httpRequestLog.getRequestBody();

        try {

            BalanceDto dto = HttpService.convertJsonToDto(body, BalanceDto.class);
            String reqId = dto.getReqId();

            // 1. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getUserToken());
            this.doVerification(dto, gameSession);

            // 3. Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession);
            BalanceDataWalletVo balanceDataWalletVo = new BalanceDataWalletVo();

            // 4. Set Balance and Currency
            balanceDataWalletVo.setBalance(balance);
            balanceDataWalletVo.setCurrency(gameSession.getCurrencyCode());

            // 5. Set BalanceDataWalletVo Object
            balanceDataVo.setWallet(balanceDataWalletVo);
            balanceVo.setReqId(reqId);
            balanceVo.setStatus(HttpStatus.SC_OK);

        } catch (InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException |
                 DisabledVendorLineException |
                 InvalidPlayerException e
        ) {
            balanceErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            balanceVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            balanceErrorVo.setCode(ResponseCodes.USER_TOKEN_NOT_FOUND_OR_INVALID);
            balanceVo.setStatus(HttpStatus.SC_UNAUTHORIZED);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledAgentPlayerException e) {
            balanceErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            balanceVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (GameNotSupportedException e) {
            balanceErrorVo.setCode(ResponseCodes.GAME_NOT_FOUND);
            balanceVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } catch (DisabledGameException e) {
            balanceErrorVo.setCode(ResponseCodes.GAME_NOT_AVAILABLE);
            balanceVo.setStatus(HttpStatus.SC_FORBIDDEN);
            httpService.logError(httpRequestLog, e);
        } catch (InvalidRequestException |
                 CurrencyNotSupportedException e
        ) {
            balanceErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            balanceVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, e);
        } catch (JsonProcessingException e) {
            balanceErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            balanceVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } finally {
            if(balanceVo.getStatus() == HttpStatus.SC_OK) {
                balanceVo.setData(balanceDataVo);
            } else {
                balanceErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(balanceErrorVo.getCode()));
                balanceVo.setError(balanceErrorVo);
            }
            httpService.end(httpRequestLog, balanceVo);
        }

        return balanceVo;

    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession)
            throws AuthenticationException,
            InvalidPlayerException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException {

        // Verify received token is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getToken(), dto.getUserToken(), AuthenticationException::new);

        // Verify received username is the same from game session
        // ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserId(), InvalidPlayerException::new);
        if(!gameSession.getVendorPlayerUsername().equals(dto.getUserId())) {
            throw new InvalidPlayerException();
        }

        // Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), GameNotSupportedException::new);

        // Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
