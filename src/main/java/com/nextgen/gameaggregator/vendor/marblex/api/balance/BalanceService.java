package com.nextgen.gameaggregator.vendor.marblex.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
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

    @Autowired
    public BalanceService(HttpService httpService, GameSessionService gameSessionService, WalletService walletService, VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.vendorService = vendorService;
    }

    public CommonVo getBalance(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        CommonVo commonVo = new CommonVo();
        CommonDto commonDto = new CommonDto();

        try {
            commonDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), CommonDto.class);

            ValidationUtils.validateRequest(commonDto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(commonDto.getPlayerId());

            vendorService.doVerification(commonDto, gameSession);

            BigDecimal balance = walletService.getBalance(httpRequestLog.getId(), gameSession, httpRequestLog);

            commonVo = vendorService.mapToSuccess(commonDto, balance);

        } catch (Exception exception){
            httpService.logError(httpRequestLog,exception);
        } finally {
            commonVo.setTraceId(commonDto.getTraceId());
            httpService.end(httpRequestLog,commonVo);
        }
        return commonVo;
    }

}
