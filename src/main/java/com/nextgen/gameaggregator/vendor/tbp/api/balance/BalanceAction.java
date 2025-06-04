package com.nextgen.gameaggregator.vendor.tbp.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.tbp.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
                         HttpService httpService,
                         VendorService vendorService) {
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.httpService = httpService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.GETBALANCE)
    public BalanceVo getBalance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        BalanceVo responseVo = new BalanceVo();
        String body = httpRequestLog.getRequestBody();

        try {
            BalanceDto balanceDto = HttpService.convertJsonToDto(body, BalanceDto.class);

            // Get GameSession with username
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getPlayerId());

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // Retrieve the latest wallet balance from Operator
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            responseVo.setBalance(balance.setScale(2, RoundingMode.DOWN));
            responseVo.setError(ResponseCode.OK);

        } catch (Exception e) {
            this.handleException(e, responseVo, httpRequestLog);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doValidation(BalanceDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession)
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, CredentialNotFoundException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. verify Username, Password, PlayerId, Currency, SessionId
        vendorService.validate(dto.getUsername(), dto.getPassword(), dto.getPlayerId(), dto.getCurrency(), dto.getSessionId(), gameSession);
    }


    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class})
    private void handleException(Exception e, BalanceVo responseVo, HttpRequestLog httpRequestLog) {

        if (e instanceof InvalidRequestException) {
            responseVo.setError(ResponseCode.UNEXPECTED_INPUT);
        } else if (e instanceof AuthenticationException) {
            responseVo.setError(ResponseCode.PERMISSION_DENIED);
        } else {
            responseVo.setError(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        httpService.logError(httpRequestLog, e);
    }
}
