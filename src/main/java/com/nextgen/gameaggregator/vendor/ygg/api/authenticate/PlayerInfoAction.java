package com.nextgen.gameaggregator.vendor.ygg.api.authenticate;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.DisabledAgentPlayerException;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.ygg.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ygg.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.ygg.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class PlayerInfoAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @Autowired
    public PlayerInfoAction(HttpService httpService, GameSessionService gameSessionService,
                            VendorLineService vendorLineService, AgentPlayerService agentPlayerService,
                            VendorGameService vendorGameService, WalletService walletService,
                            VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.walletService = walletService;
        this.vendorService = vendorService;
    }

    @GetMapping(path = EndPoints.AUTHENTICATE)
    public PlayerInfoVo authenticate(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        PlayerInfoDto dto = new PlayerInfoDto();

        PlayerInfoVo responseVo = new PlayerInfoVo();
        try {
            // Log request body
            httpRequestLog.setRequestBody(request.getQueryString());

            // Convert vendor query string to DTO
            dto = HttpService.convertQueryStringToDto(request.getQueryString(), PlayerInfoDto.class);

            // Validate DTO.
            ValidationUtils.validateRequest(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.verifyToken(dto.getSessionToken());

            // Do verification
            doVerification(gameSession);

            // Get player wallet balance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            DataVo data = new DataVo();
            data.setPlayerId(String.valueOf(gameSession.getVendorPlayerUsername()));
            data.setNickName(gameSession.getVendorPlayerUsername());
            data.setOrganization(dto.getOrg());
            data.setBalance(balance);
            data.setCurrency(gameSession.getVendorCurrencyCode());
            data.setHomeCurrency(gameSession.getVendorCurrencyCode());
            data.setCountry("DE");

            // Set response code and Vodata
            responseVo.setData(data);
            responseVo.setCode(ResponseCode.SUCCESS.code);

        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_LOGGED_IN);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doVerification(GameSession gameSession) throws
            DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException,
            AuthenticationException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

    }

}
