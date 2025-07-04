package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.crystal.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.crystal.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.crystal.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.crystal.service.VendorService;
import com.nextgen.gameaggregator.vendor.crystal.vo.CommonDataVo;
import com.nextgen.gameaggregator.vendor.crystal.vo.ErrorVo;
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
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final WalletService walletService;
    private final VendorService vendorService;

    public BalanceAction(HttpService httpService,
                         WalletService walletService,
                         GameSessionService gameSessionService,
                         VendorLineService vendorLineService,
                         AgentPlayerService agentPlayerService,
                         VendorService vendorService) {
        this.walletService = walletService;
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
    }

    @PostMapping(path = EndPoints.BALANCE)
    public CommonDataVo balance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        CommonDataVo commonDataVo = new CommonDataVo();

        CommonDto commonDto;
        try {
            String body = httpRequestLog.getRequestBody();
            commonDto = HttpService.convertJsonToDto(body, CommonDto.class);
            this.doValidation(commonDto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getPlayerId());

            vendorService.doCompareSignature(request, httpRequestLog, gameSession);
            this.doVerification(gameSession);

            BigDecimal getWalletBalance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            commonDataVo = this.prepareBalanceVo(getWalletBalance);

        } catch (Exception e) {
            this.handleException(e, commonDataVo, httpRequestLog);
        } finally {
            httpService.end(httpRequestLog, commonDataVo);
        }
        return commonDataVo;
    }

    private void doValidation(CommonDto commonDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(commonDto);
    }

    private void doVerification(GameSession gameSession) throws
            DisabledVendorLineException,
            DisabledAgentPlayerException {

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }


    private CommonDataVo prepareBalanceVo(BigDecimal walletBalance) {
        CommonDataVo commonDataVo = new CommonDataVo();
        commonDataVo.getData().setBalance(walletBalance.setScale(2, RoundingMode.DOWN));
        return commonDataVo;
    }

    @ExceptionHandler({InvalidRequestException.class, InvalidPlayerException.class,
            AuthenticationException.class, Exception.class})
    private void handleException(Exception e, CommonDataVo commonDataVo, HttpRequestLog httpRequestLog) {

        if (e instanceof InvalidRequestException) {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.INVALID_PARAMETERS.code,
                    ResponseCodes.INVALID_PARAMETERS.message
            ));
        } else if (e instanceof AuthenticationException) {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.INVALID_SIGNATURE.code,
                    ResponseCodes.INVALID_SIGNATURE.message
            ));
        } else {
            commonDataVo.setError(new ErrorVo(
                    ResponseCodes.PLAYER_NOT_FOUND.code,
                    ResponseCodes.PLAYER_NOT_FOUND.message
            ));
        }
        commonDataVo.setData(null);
        httpService.logError(httpRequestLog, e);
    }
}
