package com.nextgen.gameaggregator.vendor.marblex.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import com.nextgen.gameaggregator.vendor.marblex.service.VendorService;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    public final VendorLineService vendorLineService;
    public final AgentPlayerService agentPlayerService;
    public final VendorGameService vendorGameService;

    @Autowired
    public BalanceService(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, VendorService vendorService, VendorLineService vendorLineService, AgentPlayerService agentPlayerService, VendorGameService vendorGameService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
    }

    public CommonVo getBalance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        CommonVo commonVo = new CommonVo();
        BalanceDto balanceDto = new BalanceDto();

        try {
            balanceDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), BalanceDto.class);

            ValidationUtils.validateRequest(balanceDto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getPlayerId());

            this.doVerification(balanceDto, gameSession);

            BigDecimal balance = walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog);

            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), balance);

        } catch (AuthenticationException | InvalidPlayerException |
                 InvalidAgentApiCredentialException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_AUTHENTICATION);
            httpService.logError(httpRequestLog, exception);
        } catch (InvalidCurrencyException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_CURRENCY);
            httpService.logError(httpRequestLog, exception);
        } catch (InvalidRequestException exception) {
            commonVo.setStatusCode(StatusCode.INVALID_REQUEST);
            httpService.logError(httpRequestLog, exception);
        } catch (Exception exception) {
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            commonVo.setTraceId(balanceDto.getTraceId());
            httpService.end(httpRequestLog, commonVo);
        }
        return commonVo;
    }

    private void doVerification(BalanceDto dto, GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidPlayerException, InvalidCurrencyException {

        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

//        // Verify Currency from dto is equal
        ValidationUtils.isEquals(gameSession.getCurrencyCode(), dto.getCurrency(), InvalidCurrencyException::new);

        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);
    }
}
