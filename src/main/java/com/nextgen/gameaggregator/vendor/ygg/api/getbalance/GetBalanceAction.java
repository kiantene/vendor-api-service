package com.nextgen.gameaggregator.vendor.ygg.api.getbalance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
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
public class GetBalanceAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;
    private final VendorGameService vendorGameService;
    private final WalletService walletService;
    private final VendorService vendorService;

    @Autowired
    public GetBalanceAction(HttpService httpService, GameSessionService gameSessionService,
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

    @GetMapping(path = EndPoints.BALANCE)
    public GetBalanceVo getBalance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        GetBalanceDto dto = new GetBalanceDto();

        GetBalanceVo responseVo = new GetBalanceVo();

        try {
            // Log Request Body
            httpRequestLog.setRequestBody(request.getQueryString());

            // Convert query string to DTO
            dto = HttpService.convertQueryStringToDto(request.getQueryString(), GetBalanceDto.class);

            // Validate request
            ValidationUtils.validateRequest(dto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());

            // Get player wallet balance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            // Do verification
            doVerification(dto, gameSession);

            // Set Response data
            DataVo data = new DataVo();
            data.setBalance(balance);
            data.setCurrency(gameSession.getVendorCurrencyCode());
            data.setPlayerId(dto.getPlayerId());
            data.setNickName(gameSession.getVendorPlayerUsername());
            data.setApplicableBonus(BigDecimal.valueOf(0));
            data.setHomeCurrency(gameSession.getVendorCurrencyCode());
            data.setOrganization(dto.getOrg());

            // Set response data and code
            responseVo.setData(data);
            responseVo.setCode(ResponseCode.SUCCESS.code);

        } catch (InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_LOGGED_IN);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_AUTHORIZED);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }
        return responseVo;
    }

    private void doVerification(GetBalanceDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);

    }

}
