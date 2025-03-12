package com.nextgen.gameaggregator.vendor.smartsoft.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.Credentials;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.smartsoft.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.smartsoft.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import javax.security.auth.login.CredentialException;
import java.math.BigDecimal;

@Service
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
        BalanceVo balanceVo = new BalanceVo();
        String body = httpRequestLog.getRequestBody();
        String signature = request.getHeader("X-Signature");
        String sessionId = request.getHeader("X-SessionId");
        String userName = request.getHeader("X-UserName");
        String clientExternalKey = request.getHeader("X-ClientExternalKey");

        try {
            BalanceDto balanceDto = new BalanceDto();
            balanceDto.setSignature(signature);
            balanceDto.setSessionId(sessionId);
            balanceDto.setUserName(userName);
            balanceDto.setClientExternalKey(clientExternalKey);

            // Get GameSession with username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(userName);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession, httpRequestLog);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            httpRequestLog.setRequestBody("Request Body: " + body + " Request Header: " + vendorService.getHeaders(request));

            balanceVo.setCurrencyCode(gameSession.getCurrencyCode());
            balanceVo.setAmount(balance);
        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            balanceVo.setResponseCode(ResponseCode.FAIL);
        } finally {
            httpService.end(httpRequestLog, balanceVo);
        }
        return balanceVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession, HttpRequestLog httpRequestLog)
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, InvalidRequestException, CredentialNotFoundException, CredentialException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUserName(), InvalidRequestException::new);

        //5. Verify signature
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(VendorService.signatureGenerator(secretKey, httpRequestLog.getMethod(), httpRequestLog.getRequestBody()), dto.getSignature(), AuthenticationException::new);

        //6. verify ClientExternalKey
        ValidationUtils.isEquals(gameSession.getVendorPlayerId().toString(), dto.getClientExternalKey(), AuthenticationException::new);
    }
}
