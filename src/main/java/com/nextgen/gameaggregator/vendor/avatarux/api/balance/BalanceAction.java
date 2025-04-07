package com.nextgen.gameaggregator.vendor.avatarux.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Credentials;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.avatarux.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.avatarux.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
public class BalanceAction {
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final HttpService httpService;
    private final VendorService vendorService;

    public BalanceAction(GameSessionService gameSessionService,
                         WalletService walletService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorGameService vendorGameService,
                         HttpService httpService, VendorService vendorService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.httpService = httpService;
        this.vendorService = vendorService;
    }

    @GetMapping(path = EndPoints.BALANCE)
    public BalanceVo getBalance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        BalanceVo responseVo = new BalanceVo();
        String body = httpRequestLog.getRequestBody();
        httpRequestLog.setRequestBody("Request Body: \n" + body + "\nRequest Header: \n" + vendorService.getHeaders(request));

        try {
            BalanceDto balanceDto = new BalanceDto();

            // Get GameSession with username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getNativeId());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession, body);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setBalance(balance);
        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.getError().setCode(ResponseCode.PLAYER_UNAUTHORIZED.code);
            responseVo.getError().setMessage(ResponseCode.PLAYER_UNAUTHORIZED.description);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.getError().setCode(ResponseCode.UNKNOWN.code);
            responseVo.getError().setMessage(ResponseCode.UNKNOWN.description);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession, String body)
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, CredentialNotFoundException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getNativeId(), AuthenticationException::new);

        //5. Verify token
        ValidationUtils.isEquals(gameSession.getToken(), dto.getAuthorization(), AuthenticationException::new);

        //6. Verify X-Server-Authorization
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.generateHash(secretKey, body), dto.getXServerAuthorization(), AuthenticationException::new);

    }
}
