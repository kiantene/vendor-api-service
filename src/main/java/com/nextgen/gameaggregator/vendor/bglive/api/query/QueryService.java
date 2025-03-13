package com.nextgen.gameaggregator.vendor.bglive.api.query;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.AuthenticationException;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.constant.ThreadSize;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class QueryService {
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(ThreadSize.THREAD_SIZE);
    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final VendorService vendorService;

    public QueryService(HttpService httpService,
                        GameSessionService gameSessionService,
                        VendorService vendorService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
    }

    public CommonVo query(HttpRequestLog httpRequestLog, HttpServletRequest httpServletRequest) {
        CommonVo commonVo = new CommonVo();
        ExecutorService executor = Executors.newFixedThreadPool(ThreadSize.THREAD_SIZE);
        try {
            String body = httpRequestLog.getRequestBody();
            QueryDto queryDto = HttpService.convertJsonToDto(body, QueryDto.class);
            // Handle the action and return the resulting value
            this.doValidation(queryDto);
            List<CompletableFuture<QueryVo>> queryVoList = new LinkedList<>();
            for (OrdersMapDto ordersMapDto : queryDto.getParamsDto().getOrdersMapDto()) {
                GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(ordersMapDto.getOrderLoginId());
                CompletableFuture<QueryVo> queryVo = CompletableFuture.supplyAsync(() -> processData(ordersMapDto, httpServletRequest, gameSession), executor);
                queryVoList.add(queryVo);
            }
            List<QueryVo> queryList = processAndValidateQueryResponses(queryVoList);
            commonVo.setSuccessResponse(queryDto.getId(), queryList);

        } catch (InvalidRequestException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
                    ResponseCodes.AUTH_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message);
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

    private void doValidation(OrdersMapDto ordersMapDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(ordersMapDto);
    }

    private Integer checkBetAvailable(GameSession gameSession, OrdersMapDto ordersMapDto) throws
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

    private QueryVo processData(OrdersMapDto ordersMapDto, HttpServletRequest httpServletRequest, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(httpServletRequest);
        QueryVo queryVo = null;
        try {
            doValidation(ordersMapDto);

            Integer status = checkBetAvailable(gameSession, ordersMapDto);

            queryVo = new QueryVo(ordersMapDto.getOrderId(), status);

        } catch (Exception e) {
            // do nothing, return null
            httpService.logError(httpRequestLog, e);
        }
        return queryVo;
    }

    private List<QueryVo> processAndValidateQueryResponses(List<CompletableFuture<QueryVo>> queryVoList) throws BetNotFoundException {
        List<QueryVo> queryList = VendorService.processMultipleDataResponds(queryVoList);

        if (queryList.contains(null)) {
            throw new BetNotFoundException("Some query responses are null");
        }
        return queryList;
    }
}