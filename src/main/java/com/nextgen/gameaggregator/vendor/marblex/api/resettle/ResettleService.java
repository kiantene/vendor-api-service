package com.nextgen.gameaggregator.vendor.marblex.api.resettle;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.service.VendorService;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.jvnet.hk2.annotations.Service;

@Service
public class ResettleService {
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final SportWalletService sportWalletService;

    public ResettleService(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, VendorService vendorService, WalletRequestService walletRequestService, SportWalletService sportWalletService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.sportWalletService = sportWalletService;
    }

    public CommonVo settleBet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        ResettleDto resettleDto = new ResettleDto();
        GameSession gameSession = new GameSession();

        try {
            resettleDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), ResettleDto.class);

            ValidationUtils.validateRequest(resettleDto);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(resettleDto.getPlayerId());

            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(resettleDto.getGameCode(), gameSession);

            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doDataMapper(walletRequest, resettleDto);

            vendorService.doVerification(resettleDto, gameSession, false);

            walletRequest = sportWalletService.resettle(walletRequest);

            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), walletRequest.getBalanceAfter());

        } catch (Exception e) {

        } finally {
            commonVo.setTraceId(resettleDto.getTraceId());
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }
}
