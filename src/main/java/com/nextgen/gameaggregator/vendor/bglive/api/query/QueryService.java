package com.nextgen.gameaggregator.vendor.bglive.api.query;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorPlayerService vendorPlayerService;
    private final VendorService vendorService;

    @Autowired
    public QueryService(HttpService httpService,
                        WalletService walletService,
                        GameSessionService gameSessionService,
                        VendorLineService vendorLineService,
                        AgentPlayerService agentPlayerService,
                        VendorPlayerService vendorPlayerService,
                        VendorService vendorService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorPlayerService = vendorPlayerService;
        this.vendorService = vendorService;
    }

    public CommonVo query(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo commonVo = new CommonVo();
        try {
            String body = httpRequestLog.getRequestBody();
            QueryDto queryDto = HttpService.convertJsonToDto(body, QueryDto.class);
            // Handle the action and return the resulting value
            this.doValidation(queryDto);


            //Check bet record available from settle and unsettle table
//            this.checkBetAvailable(gameSession, queryDto.getQueryRequestDto());
//            VendorPlayer vendorPlayer = getVendorPlayer(betDto);
//            GameSession gameSession = getGameSession(vendorPlayer);
//
//            this.doVerification(betDto, gameSession);
//            //process bet
//            processOrders(betDto, gameSession, traceId, httpRequestLog);
//
//            ResultVo resultVo = new ResultVo();
//            resultVo.setUserId(vendorPlayer.getId());
//            resultVo.setSn(betDto.getParamsDto().getSn());
//            resultVo.setAvailableAmount(walletService.getBalance(traceId, gameSession, httpRequestLog));
//            resultVo.setOrderResult("1");
//            String tranId = betDto.getParamsDto().getTranId();
//            resultVo.setTranId(tranId != null && !tranId.trim().isEmpty() ? tranId : "null");
//            commonVo.setSuccessResponse(betDto.getId(), resultVo);

//        } catch (InsufficientBalanceException e) {
//            //set Vo
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.INSUFFICIENT_BALANCE.code,
//                    ResponseCodes.INSUFFICIENT_BALANCE.message, ResponseCodes.INSUFFICIENT_BALANCE.message);
//            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

//        } catch (InvalidPlayerException e) {
//
//            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
//                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
//            httpService.logError(httpRequestLog, e);

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

    private void doValidation(QueryDto queryDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(queryDto);

        ParamsDto paramsDto = queryDto.getParamsDto();
        if (paramsDto != null) {
            ValidationUtils.validateRequest(paramsDto);

            List<OrdersMapDto> ordersMapDtoList = paramsDto.getOrdersMapDto();
            if (ordersMapDtoList == null || ordersMapDtoList.isEmpty()) {
                throw new InvalidRequestException("Query request must contain at least one order.");
            }
            for (OrdersMapDto ordersMapDto : ordersMapDtoList) {
                ValidationUtils.validateRequest(ordersMapDto);
            }
        }
    }

//    private void checkBetAvailable(GameSession gameSession, QueryDto queryDto) throws TransactionStillProcessingException, BetResultIdempotentViolationException {
//
//        // settle bet Idempotent Check
//        vendorService.settledBetIdempotentCheck(gameSession, queryDto.getInitialDebitTransferId(), queryRequestDto.getGameInstanceId());
//
//        // unsettle bet Idempotent Check
//        vendorService.unsettledBetIdempotentCheck(gameSession, queryDto.getTransferId(), queryRequestDto.getGameInstanceId());
//
//    }

}