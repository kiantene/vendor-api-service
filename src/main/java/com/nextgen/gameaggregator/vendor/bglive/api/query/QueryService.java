package com.nextgen.gameaggregator.vendor.bglive.api.query;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
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

    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final VendorService vendorService;

    @Autowired
    public QueryService(HttpService httpService,
                        GameSessionService gameSessionService,
                        VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
    }

    public CommonVo query(HttpRequestLog httpRequestLog) {
        CommonVo commonVo = new CommonVo();
        try {
            String body = httpRequestLog.getRequestBody();
            QueryDto queryDto = HttpService.convertJsonToDto(body, QueryDto.class);
            // Handle the action and return the resulting value
            this.doValidation(queryDto);

            List<QueryVo> orderStatusList = processOrders(queryDto);

            commonVo.setSuccessResponse(httpRequestLog.getId(), orderStatusList);

        } catch (InvalidRequestException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
            httpService.logError(httpRequestLog, e);

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

    private Integer checkBetAvailable(GameSession gameSession, OrdersMapDto ordersMapDto) throws
            TransactionStillProcessingException,
            BetResultIdempotentViolationException,
            BetNotFoundException {

        Integer status;
        String orderId = ordersMapDto.getOrderId();

        // settle bet Idempotent Check
        try {
            status = vendorService.settledBetIdempotentCheck(gameSession, orderId);
        } catch (BetNotFoundException e) {
            status = vendorService.unsettledBetIdempotentCheck(orderId);
            return status;
        }
        return status;
    }

    private List<QueryVo> processOrders(QueryDto queryDto)
            throws BetResultIdempotentViolationException,
            TransactionStillProcessingException, AuthenticationException, BetNotFoundException {

        List<QueryVo> orderStatusList = new ArrayList<>();

        for (OrdersMapDto ordersMapDto : queryDto.getParamsDto().getOrdersMapDto()) {
            queryDto.setCurrentMapOrder(ordersMapDto);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(ordersMapDto.getOrderLoginId());
            //Check bet record available from settle and unsettle table
            Integer status = checkBetAvailable(gameSession, ordersMapDto);

            QueryVo queryVo = new QueryVo();
            queryVo.setOrderId(ordersMapDto.getOrderId());
            queryVo.setStatus(status);
            orderStatusList.add(queryVo);
        }
        return orderStatusList;
    }
}