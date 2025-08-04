package com.nextgen.gameaggregator.vendor.inout.api.authenticate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;

@Service
public class AuthenticateService {
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorLineService vendorLineService;
    private final VendorGameService vendorGameService;
    private final GameSessionService gameSessionService;
    private final AgentPlayerService agentPlayerService;

    public AuthenticateService(HttpService httpService,
                               WalletService walletService,
                               VendorLineService vendorLineService,
                               VendorGameService vendorGameService,
                               GameSessionService gameSessionService,
                               AgentPlayerService agentPlayerService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.vendorLineService = vendorLineService;
        this.vendorGameService = vendorGameService;
        this.gameSessionService = gameSessionService;
        this.agentPlayerService = agentPlayerService;
    }

    public CommonVo initSession(HttpRequestLog httpRequestLog) {
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
            this.doVerification(dto.getData().getCurrency(), dto.getData().getGameMode(), gameSession);

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
        }

        return responseVo;
    }

    private void doValidation(CommonDto<AuthenticateDto> dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(String currency, String gameMode, GameSession gameSession) throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException {
        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 4. Verify Currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), currency, AuthenticationException::new);

        // 5. Verify GameMode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), gameMode, AuthenticationException::new);
    }

    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {
        if (e instanceof InvalidRequestException) {
            responseVo.setError(ResponseCode.INVALID_TOKEN);
        } else if (e instanceof AuthenticationException) {
            responseVo.setError(ResponseCode.ACCOUNT_LOCKED);
        } else if (e instanceof DisabledVendorLineException ||
                e instanceof DisabledGameException ||
                e instanceof DisabledAgentPlayerException) {
            responseVo.setError(ResponseCode.GAME_DISABLED);
        } else {
            responseVo.setError(ResponseCode.UNKNOWN_ERROR);
        }

        httpService.logError(httpRequestLog, e);
    }


}
