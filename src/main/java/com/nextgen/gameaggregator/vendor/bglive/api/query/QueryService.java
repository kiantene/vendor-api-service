package com.nextgen.gameaggregator.vendor.bglive.api.query;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.TransactionStillProcessingException;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public CommonVo query(HttpRequestLog httpRequestLog) {
        CommonVo commonVo = new CommonVo();
        try {
            String body = httpRequestLog.getRequestBody();
            QueryDto queryDto = HttpService.convertJsonToDto(body, QueryDto.class);
            // Handle the action and return the resulting value
            this.doValidation(queryDto);

            GameSession gameSession = getGameSession(queryDto);
            List<QueryVo> orderStatusList = processOrders(queryDto, gameSession);

            commonVo.setSuccessResponse(httpRequestLog.getId(), orderStatusList);
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

    private void checkBetAvailable(GameSession gameSession, OrdersMapDto ordersMapDto) throws TransactionStillProcessingException, BetResultIdempotentViolationException {

        String orderId = ordersMapDto.getOrderId();
//        // settle bet Idempotent Check
        vendorService.settledBetIdempotentCheck(gameSession, orderId, orderId);

        // unsettle bet Idempotent Check
        vendorService.unsettledBetIdempotentCheck(orderId);

    }

    private List<QueryVo> processOrders(QueryDto queryDto, GameSession gameSession)
            throws BetResultIdempotentViolationException,
            TransactionStillProcessingException {

        List<QueryVo> orderStatusList = new ArrayList<>();

        for (OrdersMapDto ordersMapDto : queryDto.getParamsDto().getOrdersMapDto()) {
            queryDto.setCurrentMapOrder(ordersMapDto);

            //Check bet record available from settle and unsettle table
            this.checkBetAvailable(gameSession, ordersMapDto);
//            orderStatusList.add(new QueryVo(ordersMapDto.getOrderId(), status));
        }
        return orderStatusList;
    }

    private GameSession getGameSession(QueryDto queryDto) throws AuthenticationException {
        return gameSessionService.getGameSessionByVendorPlayerUsername(queryDto.getParamsDto().getLoginId());
    }
}