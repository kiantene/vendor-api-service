package com.nextgen.gameaggregator.vendor.tbp.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.tbp.constant.Credentials;
import com.nextgen.gameaggregator.vendor.tbp.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.tbp.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.tbp.service.VendorService;
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
            responseVo.setErrorCode(ResponseCode.OK.code);
            responseVo.setErrorMessage(ResponseCode.OK.description);
            responseVo.setSuccessful(true);

        } catch (InvalidRequestException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setErrorCode(ResponseCode.UNEXPECTED_INPUT.code);
            responseVo.setErrorMessage(ResponseCode.UNEXPECTED_INPUT.description);
            responseVo.setSuccessful(false);

        } catch (AuthenticationException e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setErrorCode(ResponseCode.PERMISSION_DENIED.code);
            responseVo.setErrorMessage(ResponseCode.PERMISSION_DENIED.description);
            responseVo.setSuccessful(false);

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            responseVo.setErrorCode(ResponseCode.INTERNAL_SERVER_ERROR.code);
            responseVo.setErrorMessage(ResponseCode.INTERNAL_SERVER_ERROR.description);
            responseVo.setSuccessful(false);
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
            throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, AuthenticationException, CredentialNotFoundException, InvalidRequestException {

        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify Username
        String username = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        ValidationUtils.isEquals(username, dto.getUsername(), AuthenticationException::new);

        // 5. Verify Password
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.TOKEN);
        ValidationUtils.isEquals(password, dto.getPassword(), AuthenticationException::new);

        // 6. Verify UserId
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), AuthenticationException::new);

        // 7. Verify Currency
        ValidationUtils.isEquals(gameSession.getCurrencyCode(), dto.getCurrency(), AuthenticationException::new);

        // 8. Verify SessionId
        ValidationUtils.isEquals(gameSession.getVendorToken(), dto.getSessionId(), AuthenticationException::new);
    }
}
