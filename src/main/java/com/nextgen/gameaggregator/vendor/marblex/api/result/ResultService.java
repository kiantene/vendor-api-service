package com.nextgen.gameaggregator.vendor.marblex.api.result;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import com.nextgen.gameaggregator.vendor.marblex.service.VendorService;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultService {
    public final HttpService httpService;
    public final GameSessionService gameSessionService;
    public final WalletService walletService;
    public final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final SportWalletService sportWalletService;

    @Autowired
    public ResultService(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, VendorService vendorService, WalletRequestService walletRequestService, SportWalletService sportWalletService) {
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
        ResultDto resultDto = new ResultDto();

        try {
            resultDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), ResultDto.class);

            ValidationUtils.validateRequest(resultDto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(resultDto.getPlayerId());

            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            vendorService.doDataMapper(walletRequest, resultDto);

            vendorService.doVerification(resultDto, gameSession, false);

            walletRequest = sportWalletService.settle(walletRequest);

            commonVo = vendorService.mapToSuccess(resultDto, walletRequest.getBalanceAfter());

        } catch (Exception exception){
            commonVo.setStatusCode(StatusCode.VENDOR_API_ERROR);
            httpService.logError(httpRequestLog,exception);
        } finally {
            commonVo.setTraceId(resultDto.getTraceId());
            httpService.end(httpRequestLog,commonVo);
        }
        return commonVo;
    }
}
