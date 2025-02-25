package com.nextgen.gameaggregator.vendor.bglive.api.transfer;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.bglive.api.settlement.SettleDto;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final com.nextgen.gameaggregator.vendor.bglive.service.VendorService vendorService;

    @Autowired
    public TransferService(HttpService httpService,
                           WalletService walletService,
                           GameSessionService gameSessionService,
                           VendorLineService vendorLineService,
                           AgentPlayerService agentPlayerService,
                           VendorService vendorService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
    }

    public CommonVo transfer(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        try {
            String body = httpRequestLog.getRequestBody();
            SettleDto settleDto = HttpService.convertJsonToDto(body, SettleDto.class);
            // Handle the action and return the resulting value
//            this.doValidation(settleDto);
//
//            GameSession gameSession = getGameSession(settleDto);
//            this.doVerification(settleDto, gameSession);
//
//            processSettleOrders(settleDto, gameSession, traceId, httpRequestLog);
//
            ResultVo resultVo = new ResultVo();
//            resultVo.setUserId(gameSession.getVendorPlayerId());
//            resultVo.setSn(settleDto.getParamsDto().getSn());
//            resultVo.setAvailableAmount(walletService.getBalance(traceId, gameSession, httpRequestLog));
//            resultVo.setOrderResult("1");
//            String tranId = settleDto.getParamsDto().getTranId();
//            resultVo.setTranId((tranId == null || tranId.trim().isEmpty()) ? null : tranId);

            commonVo.setSuccessResponse(settleDto.getId(), resultVo);
//
//        } catch (InsufficientBalanceException e) {
//            //set Vo
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.INSUFFICIENT_BALANCE.code,
//                    ResponseCodes.INSUFFICIENT_BALANCE.message, ResponseCodes.INSUFFICIENT_BALANCE.message);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (InvalidRequestException e) {
//            //set Vo
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
//                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (InvalidPlayerException e) {
//
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
//                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
//            httpService.logError(httpRequestLog, e);
//
//        } catch (AuthenticationException e) {
//
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
//                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
//            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        }
        return commonVo;
    }
}

