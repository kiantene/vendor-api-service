package com.nextgen.gameaggregator.vendor.inout.api.authenticate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.inout.constant.Credentials;
import com.nextgen.gameaggregator.vendor.inout.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.inout.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.inout.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;

@Service
public class AuthenticateService {
    private final HttpService httpService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;

    public AuthenticateService(HttpService httpService,
                               VendorLineService vendorLineService,
                               GameSessionService gameSessionService,
                               WalletService walletService) {
        this.httpService = httpService;
        this.vendorLineService = vendorLineService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
    }

    public CommonVo authenticate(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
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
            this.doVerification(dto, gameSession);

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

    private void doVerification(CommonDto<AuthenticateDto> dto, GameSession gameSession) throws AuthenticationException, CredentialNotFoundException {
        if (gameSession.getStatus() == 0) throw new AuthenticationException();

        // 1. Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // 2. Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // 3. Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // 1. Verify Username
        String username = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        ValidationUtils.isEquals(username, dto.getUsername(), AuthenticationException::new);

        // 2. Verify Password
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.TOKEN);
        ValidationUtils.isEquals(password, dto.getPassword(), AuthenticationException::new);

        // 3. Verify PlayerId
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), AuthenticationException::new);

        // 4. Verify DefenceCode
        ValidationUtils.isEquals(gameSession.getToken(), dto.getDefenceCode(), AuthenticationException::new);
    }

    @ExceptionHandler({InvalidRequestException.class, AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonVo responseVo, HttpRequestLog httpRequestLog) {

        if (e instanceof InvalidRequestException) {
            responseVo.setError(ResponseCode.);
        } else if (e instanceof AuthenticationException) {
            responseVo.setError(ResponseCode.PERMISSION_DENIED);
        } else {
            responseVo.setError(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        httpService.logError(httpRequestLog, e);
    }

}
