package com.nextgen.gameaggregator.vendor.marblex.api.cancel;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.marblex.service.VendorService;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class CancelService {
    public final HttpService httpService;
    public final VendorService vendorService;
    public final GameSessionService gameSessionService;
    private final SportWalletService sportWalletService;
    private final WalletRequestService walletRequestService;

    public CancelService(SportWalletService sportWalletService, HttpService httpService, VendorService vendorService, GameSessionService gameSessionService, WalletRequestService walletRequestService) {
        this.sportWalletService = sportWalletService;
        this.httpService = httpService;
        this.vendorService = vendorService;
        this.gameSessionService = gameSessionService;
        this.walletRequestService = walletRequestService;
    }

    public CommonVo cancel(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        CommonVo commonVo = new CommonVo();
        CancelDto cancelDto = new CancelDto();
        GameSession gameSession = new GameSession();
        try {

            cancelDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), CancelDto.class);

            gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(cancelDto.getPlayerId());
            vendorService.doVerification(cancelDto, gameSession, false);
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doDataMapper(walletRequest, cancelDto);
            walletRequest = sportWalletService.unsettle(walletRequest);
            walletRequest = sportWalletService.refund(walletRequest);
            commonVo = vendorService.mapToSuccess(gameSession.getVendorCurrencyCode(), walletRequest.getBalanceAfter());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return commonVo;
    }


}
