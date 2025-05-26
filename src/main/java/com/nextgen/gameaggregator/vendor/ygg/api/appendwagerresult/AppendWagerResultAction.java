package com.nextgen.gameaggregator.vendor.ygg.api.appendwagerresult;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
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
public class AppendWagerResultAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final WalletService walletService;
    private final VendorGameService vendorGameService;
    private final VendorLineService vendorLineService;
    private final AgentPlayerService agentPlayerService;

    @Autowired
    public AppendWagerResultAction(HttpService httpService, GameSessionService gameSessionService,
                                   VendorService vendorService,
                                   WalletService walletService, VendorGameService vendorGameService,
                                   VendorLineService vendorLineService,
                                   AgentPlayerService agentPlayerService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.walletService = walletService;
        this.vendorGameService = vendorGameService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
    }

    @GetMapping(path = EndPoints.BONUS_GAME)
    public AppendWagerResultVo appendWagerResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        AppendWagerResultDto dto = new AppendWagerResultDto();

        AppendWagerResultVo responseVo = new AppendWagerResultVo();
        try {
            // Set request body in OpenSearch.
            httpRequestLog.setRequestBody(request.getQueryString());

            // Get request body and set to DTO.
            dto = HttpService.convertQueryStringToDto(request.getQueryString(), AppendWagerResultDto.class);

            // Validate DTO.
            ValidationUtils.validateRequest(dto);

            // Verify game session with playerID.
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getPlayerId());

            // Do verification.
            doVerification(dto, gameSession);

            // Calculate jackpot.
            ResultType resultType = vendorService.calculateResultType(dto.getBetAmount(), dto.getWinAmount(),
                    dto.getJackpotAmount(), true);

            // Get latest balance.
            BigDecimal balance = walletService.processBetResult(traceId, gameSession,
                    dto, resultType, vendorService, httpRequestLog);

            // Set response data.
            DataVo data = new DataVo();
            data.setBalance(balance);
            data.setCurrency(gameSession.getVendorCurrencyCode());
            data.setPlayerId(dto.getPlayerId());
            data.setNickName(gameSession.getVendorPlayerUsername());
            data.setApplicableBonus(BigDecimal.valueOf(0));
            data.setHomeCurrency(gameSession.getVendorCurrencyCode());
            data.setOrganization(dto.getOrg());

            // Set response code and Vodata
            responseVo.setData(data);
            responseVo.setCode(ResponseCode.SUCCESS.code);

        } catch (InvalidPlayerException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_LOGGED_IN);
            httpService.logError(httpRequestLog, e);
        } catch (AuthenticationException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_NOT_AUTHORIZED);
            httpService.logError(httpRequestLog, e);
        } catch (InsufficientBalanceException e) {
            responseVo.setResponseCode(ResponseCode.ERROR_OVERDRAFT);
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            responseVo.setResponseCode(ResponseCode.ERROR);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private void doVerification(AppendWagerResultDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, AuthenticationException {
        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

        // Verify token status is active
        vendorService.verifyTokenStatus(gameSession.getStatus());

        // Verify currency
        vendorService.verifyCurrency(gameSession.getVendorCurrencyCode(), dto.getCurrency());

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);

    }
}
