package com.nextgen.gameaggregator.vendor.inout.api.authenticate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.service.VendorService;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;

@Service
public class AuthenticateService {
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorService vendorService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final AgentPlayerService agentPlayerService;

    public AuthenticateService(HttpService httpService,
                               WalletService walletService,
                               VendorService vendorService,
                               VendorLineService vendorLineService,
                               VendorGameService vendorGameService,
                               GameSessionService gameSessionService,
                               AgentPlayerService agentPlayerService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.agentPlayerService = agentPlayerService;
    }

    public CommonVo initSession(HttpRequestLog httpRequestLog, String xSign) {
        String traceId = httpRequestLog.getId();
        String body = httpRequestLog.getRequestBody();
        CommonVo responseVo = new CommonVo();

        try {
            // 1. Retrieve request body and convert into dto
            CommonDto<AuthenticateDto> dto = HttpService.convertJsonToDto(body, new TypeReference<>() {
            });

            // 2. Validate request parameters (Non-database calls)
            this.doValidation(dto);

            // 3. Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getToken());

            // 4. Verify remaining parameters (Verify against database values)
            this.doVerification(dto.getData().getCurrency(), dto.getData().getGameMode(), xSign, body, gameSession);

            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // 5. Set response data
            responseVo.setCode(ResponseCode.OK.code);
            responseVo.setUserId(gameSession.getVendorPlayerUsername());
            responseVo.setNickname(gameSession.getAgentPlayerUsername());
            responseVo.setBalance(balance.toString());
            responseVo.setCurrency(gameSession.getVendorCurrencyCode());
            responseVo.setOperator(dto.getData().getOperator());
        } catch (Exception e) {
            this.handleException(e, responseVo, httpRequestLog);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doValidation(CommonDto<AuthenticateDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(String currency, String gameMode, String xSign, String body, GameSession gameSession) throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            CredentialNotFoundException {
        if (gameSession.getStatus() == 0) throw new AuthenticationException();
        // 1. Verify X-SIGNATURE
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET_KEY);
        ValidationUtils.isEquals(xSign, VendorService.hashHMACSha256(body, secretKey), AuthenticationException::new);

        // 2. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 3. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 4. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 5. Verify Currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), currency, AuthenticationException::new);

        // 6. Verify GameMode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), gameMode, AuthenticationException::new);
    }

    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {
        vendorService.exceptionHandler(e, responseVo);
        httpService.logError(httpRequestLog, e);
    }
}
