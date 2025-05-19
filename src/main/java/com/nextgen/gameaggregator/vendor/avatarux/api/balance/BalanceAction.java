package com.nextgen.gameaggregator.vendor.avatarux.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Credentials;
import com.nextgen.gameaggregator.vendor.avatarux.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.avatarux.constant.Headers;
import com.nextgen.gameaggregator.vendor.avatarux.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.avatarux.service.VendorService;
import com.nextgen.gameaggregator.vendor.avatarux.vo.ErrorVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    @PostMapping(path = EndPoints.BALANCE)
    public BalanceVo getBalance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        BalanceVo responseVo = new BalanceVo();
        String body = httpRequestLog.getRequestBody();
        String serverAuthorization = request.getHeader(Headers.SERVER_AUTHORIZATION);
        String authorization = request.getHeader(Headers.AUTHORIZATION);
        httpRequestLog.setRequestBody("Request Body: \n" + body + "\nRequest Header: \n" + vendorService.getHeaders(request));

        try {
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);
            balanceDto.setXServerAuthorization(serverAuthorization);
            balanceDto.setAuthorization(authorization);

            // Get GameSession with username
            GameSession gameSession = gameSessionService.verifyToken(balanceDto.getAuthorization().substring(7));
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(balanceDto.getGame(), gameSession);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession, body);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(new ErrorVo());
            responseVo.getError().setCode(ResponseCode.SERVER_UNAUTHORIZED.code);
            responseVo.getError().setMessage(ResponseCode.SERVER_UNAUTHORIZED.description);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setError(new ErrorVo());
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
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, CredentialNotFoundException, InvalidRequestException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getNativeId());

        // 5. Verify provider
        String provider = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PROVIDER);
        ValidationUtils.isEquals(provider, dto.getProvider());

        // 6. Verify Authorization
        String authorizationToken = dto.getAuthorization();
        if (authorizationToken == null || !authorizationToken.startsWith("Bearer ")) {
            throw new AuthenticationException();
        }
        String token = authorizationToken.substring(7);
        ValidationUtils.isEquals(gameSession.getToken(), token, AuthenticationException::new);

        // 7. Verify X-Server-Authorization
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.generateHash(secretKey, body), dto.getXServerAuthorization(), AuthenticationException::new);

    }
}
