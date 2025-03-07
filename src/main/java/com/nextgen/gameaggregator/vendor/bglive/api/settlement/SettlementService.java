package com.nextgen.gameaggregator.vendor.bglive.api.settlement;


import com.google.gson.Gson;
import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.scheduler.betaction.GeneralSettleDto;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bglive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.bglive.constant.ResponseCodes;
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
    private static final Integer THREAD_SIZE = 32;
    public static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(THREAD_SIZE);
    private final AgentPlayerService agentPlayerService;
    private final VendorLineService vendorLineService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final HttpService httpService;
    private final VendorService vendorService;
    private final WalletRequestService walletRequestService;
    private final OperatorWalletService operatorWalletService;
    private final BetActionLogService betActionLogService;
    private final RequestIdempotentLogService requestIdempotentLogService;

    @Autowired
    public SettlementService(HttpService httpService,
                             WalletService walletService,
                             GameSessionService gameSessionService,
                             VendorLineService vendorLineService,
                             AgentPlayerService agentPlayerService,
                             VendorService vendorService, WalletRequestService walletRequestService, OperatorWalletService operatorWalletService, BetActionLogService betActionLogService, RequestIdempotentLogService requestIdempotentLogService) {
        this.httpService = httpService;
        this.walletService = walletService;
        this.gameSessionService = gameSessionService;
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorService = vendorService;
        this.walletRequestService = walletRequestService;
        this.operatorWalletService = operatorWalletService;
        this.betActionLogService = betActionLogService;
        this.requestIdempotentLogService = requestIdempotentLogService;
    }

    public CommonVo settle(HttpRequestLog httpRequestLog, HttpServletRequest request) {
        CommonVo commonVo = new CommonVo();
        String traceId = httpRequestLog.getId();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_SIZE);
        try {
            WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);
            String body = httpRequestLog.getRequestBody();
            SettleDto settleDto = HttpService.convertJsonToDto(body, SettleDto.class);
            // Handle the action and return the resulting value
            this.doValidation(settleDto);

            GameSession gameSession = getGameSession(settleDto.getParamsDto().getLoginId());
            this.doVerification(settleDto, gameSession);

//            processSettleOrders(walletRequest, settleDto, gameSession, traceId, httpRequestLog);
            List<CompletableFuture<ResultVo>> balanceList = new LinkedList<>();
            for (OrdersDto order : settleDto.getParamsDto().getOrders()) {
                CompletableFuture<ResultVo> balance = CompletableFuture.supplyAsync(() -> processData(settleDto.getParamsDto(), order, request, gameSession), executor);
                balanceList.add(balance);
            }
            BigDecimal balance = vendorService.checkResponseAndReturnBalance(balanceList);
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

//    private ResultType calculateResultType(OrdersDto ordersDto) {
//        return vendorService.calculateResultType(settleDto.getBetAmount(), settleDto.getWinAmount(),
//                settleDto.getJackpotAmount(), false);
//    }

//    private void processSettleOrders(WalletRequest walletRequest, SettleDto settleDto, GameSession gameSession, String traceId, HttpRequestLog httpRequestLog) throws
//            InvalidAgentApiCredentialException,
//            VendorCurrencyNotSupportException,
//            BetResultIdempotentViolationException,
//            MergedBetDataIntegrityException,
//            InsufficientBalanceException,
//            TransactionStillProcessingException,
//            BetNotFoundException,
//            InvalidOperatorResponseException,
//            InternalServerTimeoutRetryException,
//            InternalServerException,
//            BetNotAllowedException {
//
////        Thread.sleep(31000);
//        for (OrdersDto order : settleDto.getParamsDto().getOrders()) {
//            settleDto.setCurrentOrder(order);
//            boolean isBullBullGame = order.getGameId().equals(GameCode.BULL_BULL);
//
//            ResultType resultType = calculateResultType(order.getValidAmount(), order.getAmount(), order.getJackpotAmount(), false);
//            walletService.processBetResult(traceId, gameSession, settleDto, resultType, vendorService, httpRequestLog);
//
//        }
//    }

    private ResultVo processData(ParamsDto paramsDto, OrdersDto ordersDto, HttpServletRequest httpServletRequest, GameSession gameSession) {

        HttpRequestLog httpRequestLog = httpService.start(httpServletRequest);
        String traceId = httpRequestLog.getId();
        BigDecimal balance = BigDecimal.ZERO;
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


//    private void dataCreditMapper(WalletRequest walletRequest, SettleDto settleDto, GameSession gameSession) {
//
//        walletRequestService.updateByGameSession(walletRequest, gameSession);
//        walletRequest.setVendorPlayerUsername(gameSession.getVendorPlayerUsername());
//        walletRequest.setExternalTransactionId(settleDto.getRoundId());
//        walletRequest.setRoundId(settleDto.getRoundId());
//        walletRequest.setVendorGameCode(gameSession.getVendorGameCode());
//        walletRequest.setTimestamp(System.currentTimeMillis());
//        walletRequest.setToken(gameSession.getToken());
//        walletRequest.setVendorBetId(settleDto.getVendorBetId());
//        walletRequest.setTakeAll(0);
//        BigDecimal amount = settleDto.getParamsDto().getAmount().abs();
//        walletRequest.setTransferAmount(amount);
//        walletRequest.setBetAmount(amount);
//
//        ResultType resultType = vendorService.calculateResultType(null, amount, settleDto.getJackpotAmount(),
//                false);
//
//        walletRequest.setWinAmount(amount);
//        walletRequest.setEffectiveTurnover(BigDecimal.ZERO);
//        walletRequest.setJackpotAmount(settleDto.getJackpotAmount());
//        walletRequest.setResultType(resultType.code);
//        walletRequest.setVendorBetTime(System.currentTimeMillis());
//        walletRequest.setVendorSettleTime(System.currentTimeMillis());
//    }

}