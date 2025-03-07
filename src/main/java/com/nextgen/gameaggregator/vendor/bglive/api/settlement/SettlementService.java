package com.nextgen.gameaggregator.vendor.bglive.api.settlement;


import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralSettleDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.bglive.constant.ThreadSize;
import com.nextgen.gameaggregator.vendor.bglive.service.VendorService;
import com.nextgen.gameaggregator.vendor.bglive.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.bglive.vo.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SettlementService {
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(ThreadSize.THREAD_SIZE);
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final BetActionLogService betActionLogService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    @Autowired
    public SettlementService(HttpService httpService,
                             WalletService walletService,
                             GameSessionService gameSessionService,
                             VendorLineService vendorLineService,
                             AgentPlayerService agentPlayerService,
                             VendorService vendorService,
                             BetActionLogService betActionLogService,
                             RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.betActionLogService = betActionLogService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        CommonVo commonVo = new CommonVo();
        ExecutorService executor = Executors.newFixedThreadPool(ThreadSize.THREAD_SIZE);
        try {
            Thread.sleep(50000);
            String body = httpRequestLog.getRequestBody();
            SettleDto settleDto = HttpService.convertJsonToDto(body, SettleDto.class);
            this.doValidation(settleDto);
            GameSession gameSession = this.getGameSession(settleDto.getParamsDto().getLoginId());
            this.doVerification(settleDto, gameSession);

            List<CompletableFuture<ResultVo>> balanceList = new LinkedList<>();
            for (OrdersDto order : settleDto.getParamsDto().getOrders()) {
                CompletableFuture<ResultVo> balance = CompletableFuture.supplyAsync(() -> processData(settleDto.getParamsDto(), order, request, gameSession), executor);
                balanceList.add(balance);
            }
            BigDecimal balance = vendorService.checkSettleResponseAndReturnBalance(balanceList);
            if (balance == null) {
                throw new BetResultNotFoundException("Have Transaction Failed");
            }
            ResultVo resultVo = new ResultVo();
            resultVo.setUserId(gameSession.getVendorPlayerId());
            resultVo.setSn(settleDto.getParamsDto().getSn());
            resultVo.setAvailableAmount(balance);
            resultVo.setOrderResult("1");
            String tranId = settleDto.getParamsDto().getTranId();
            resultVo.setTranId((tranId == null || tranId.trim().isEmpty()) ? null : tranId);

            commonVo.setSuccessResponse(settleDto.getId(), resultVo);

        } catch (InsufficientBalanceException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.INSUFFICIENT_BALANCE.code,
                    ResponseCodes.INSUFFICIENT_BALANCE.message, ResponseCodes.INSUFFICIENT_BALANCE.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidRequestException e) {
            //set Vo
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.MISSING_PARAMETERS.code,
                    ResponseCodes.MISSING_PARAMETERS.message, ResponseCodes.MISSING_PARAMETERS.message);
            httpService.logError(httpRequestLog, e);

        } catch (InvalidPlayerException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.PLAYER_INVALID.code,
                    ResponseCodes.PLAYER_INVALID.message, ResponseCodes.PLAYER_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (AuthenticationException e) {

            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.AUTH_INVALID.code,
                    ResponseCodes.AUTH_INVALID.message, ResponseCodes.AUTH_INVALID.message);
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            commonVo.setErrorResponse(httpRequestLog.getId(), ResponseCodes.SYSTEM_ERROR.code,
                    ResponseCodes.SYSTEM_ERROR.message, ResponseCodes.SYSTEM_ERROR.message);
            httpService.logError(httpRequestLog, e);

        } finally {
            // close executor
            executor.shutdown();
        }
        return commonVo;
    }

    private void doValidation(SettleDto settleDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(settleDto);

        ParamsDto paramsDto = settleDto.getParamsDto();
        if (paramsDto != null) {
            ValidationUtils.validateRequest(paramsDto);

            List<OrdersDto> ordersList = paramsDto.getOrders();
            if (ordersList == null || ordersList.isEmpty()) {
                throw new InvalidRequestException("Settle request must contain at least one order.");
            }
            for (OrdersDto order : ordersList) {
                ValidationUtils.validateRequest(order);
            }
        }
    }

    private void doValidation(OrdersDto ordersDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(ordersDto);
    }

    private void doVerification(SettleDto settleDto, GameSession gameSession) throws AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            InvalidPlayerException,
            CredentialNotFoundException,
            InvalidFormatException {

        String snCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SN_CODE);
        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.API_KEY);
        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(snCode, settleDto.getParamsDto().getSn(), InvalidPlayerException::new);

        String validateSign = VendorService.encryptBetMd5Key(settleDto.getParamsDto().getRandom(), snCode,
                gameSession.getVendorPlayerUsername(), String.valueOf(settleDto.getParamsDto().getAmount()), secretKey);
        ValidationUtils.isEquals(validateSign, settleDto.getParamsDto().getSign(), AuthenticationException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());
        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());
    }

    private GameSession getGameSession(String loginId) throws AuthenticationException {
        return gameSessionService.getGameSessionByVendorPlayerUsername(loginId);
    }

    private ResultVo processData(ParamsDto paramsDto, OrdersDto ordersDto, HttpServletRequest httpServletRequest, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(httpServletRequest);
        String traceId = httpRequestLog.getId();
        BigDecimal balance;
        boolean isRequestExists = false;
        ResultType resultType = null;
        ResultVo resultVo = null;
        try {
            this.doValidation(ordersDto);

            // Request idempotent checking for this transaction
            if (requestIdempotentLogService.checkExists(ordersDto, paramsDto.getLoginId()) == null) {
                requestIdempotentLogService.create(ordersDto, paramsDto.getLoginId());
            } else {
                isRequestExists = true;
                throw new TransactionStillProcessingException();
            }
            // Process Result
            resultType = vendorService.calculateResultType(ordersDto.getBetAmount(), ordersDto.getAmount(), ordersDto.getJackpotAmount(), false);
            balance = walletService.processBetResult(traceId, gameSession, ordersDto, resultType, vendorService, httpRequestLog);

            resultVo = new ResultVo(balance, httpRequestLog.getOperatorTimestamp());
        } catch (InsufficientBalanceException |
                 InvalidRequestException |
                 TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
        } catch (BetResultIdempotentViolationException e) {
            resultVo = new ResultVo(e.getBalance(), System.currentTimeMillis());
            httpService.logError(httpRequestLog, e);
        } catch (Exception e) {
            // do nothing, return null
            this.prepareSettleBet(ordersDto, gameSession, resultType);
            httpService.logError(httpRequestLog, e);
        } finally {
            if (!isRequestExists) {
                // first request (not request exist) will delete log after process finish.
                requestIdempotentLogService.delete(ordersDto, paramsDto.getLoginId());
            }
            httpService.end(httpRequestLog, new CommonVo());
        }
        return resultVo;
    }

    private void prepareSettleBet(OrdersDto ordersDto, GameSession gameSession, ResultType resultType) {
        THREAD_POOL.submit(() -> {
            GeneralSettleDto generalSettleDto = new ModelMapper().map(ordersDto, GeneralSettleDto.class);
            betActionLogService.create(new Gson().toJson(generalSettleDto), generalSettleDto.getRoundId(), generalSettleDto.getVendorBetId(), generalSettleDto.getExternalTransactionId(), gameSession, 2, resultType);
        });
    }
}